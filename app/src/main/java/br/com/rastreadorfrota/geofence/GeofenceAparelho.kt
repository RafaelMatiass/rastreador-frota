package br.com.rastreadorfrota.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import br.com.rastreadorfrota.auth.SessaoApp
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Situação das cercas reais no aparelho do motorista, para a tela explicar o que falta. */
sealed interface EstadoCercasAparelho {
    data object SemVeiculo : EstadoCercasAparelho
    data object SemCercas : EstadoCercasAparelho
    /** Falta localização precisa e/ou "permitir o tempo todo" (exigido pelo Android 10+). */
    data object SemPermissao : EstadoCercasAparelho
    data class Ativas(val quantidade: Int) : EstadoCercasAparelho
    data class Erro(val mensagem: String) : EstadoCercasAparelho
}

/**
 * Geofencing com o GPS REAL do aparelho do motorista (GeofencingClient do Google Play Services).
 *
 * Registra as cercas que valem para o veículo dele (toda a frota, o tipo dele ou a placa dele)
 * e reregistra quando as cercas, o veículo ou a permissão mudam. Quem recebe as transições é o
 * [GeofenceBroadcastReceiver], mesmo com o app fechado.
 *
 * Limite do Android: no máximo 100 cercas por app; elas somem ao reiniciar o aparelho e são
 * registradas de novo na próxima abertura do app.
 */
object GeofenceAparelho {

    private const val MAX_CERCAS = 100
    private var job: Job? = null
    private var ultimas: List<CercaEntity> = emptyList()

    private val _estado = MutableStateFlow<EstadoCercasAparelho>(EstadoCercasAparelho.SemVeiculo)
    val estado: StateFlow<EstadoCercasAparelho> = _estado.asStateFlow()

    fun iniciar(context: Context, escopo: CoroutineScope) {
        job?.cancel()
        val db = AppDatabase.getInstance(context)
        job = escopo.launch {
            combine(SessaoApp.usuario, db.veiculoDao().observarTodos(), db.cercaDao().observarTodas()) { usuario, veiculos, cercas ->
                val veiculo = veiculoDoMotorista(usuario, veiculos)
                veiculo to cercasDoVeiculo(veiculo, cercas)
            }
                .distinctUntilChanged()
                .collect { (veiculo, cercas) ->
                    if (veiculo != null) {
                        // O receiver pode rodar com o app fechado: guarda quem é o veículo/motorista.
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(CHAVE_PLACA, veiculo.placa)
                            .putString(CHAVE_MOTORISTA, SessaoApp.usuario.value?.nome)
                            .putString(CHAVE_UID, SessaoApp.usuario.value?.uid)
                            .apply()
                    }
                    ultimas = cercas
                    _estado.value = when {
                        veiculo == null -> EstadoCercasAparelho.SemVeiculo
                        else -> registrar(context, cercas)
                    }
                }
        }
    }

    /** Chamado quando o usuário concede a permissão (ou volta das configurações). */
    fun tentarDeNovo(context: Context, escopo: CoroutineScope) {
        if (job == null || _estado.value == EstadoCercasAparelho.SemVeiculo) return
        escopo.launch { _estado.value = registrar(context, ultimas) }
    }

    fun parar(context: Context) {
        job?.cancel()
        job = null
        ultimas = emptyList()
        _estado.value = EstadoCercasAparelho.SemVeiculo
        runCatching { LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context)) }
    }

    fun temPermissao(context: Context): Boolean {
        val precisa = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val segundoPlano = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return precisa && segundoPlano
    }

    private fun veiculoDoMotorista(usuario: Usuario?, veiculos: List<VeiculoEntity>): VeiculoEntity? {
        val remoteId = usuario?.veiculoRemoteId ?: return null
        return veiculos.find { it.remoteId == remoteId }
    }

    private fun cercasDoVeiculo(veiculo: VeiculoEntity?, cercas: List<CercaEntity>): List<CercaEntity> =
        if (veiculo == null) emptyList()
        else cercas.filter { it.ativa && it.seAplicaA(veiculo.placa, veiculo.tipo) }.take(MAX_CERCAS)

    @SuppressLint("MissingPermission") // conferida em temPermissao() antes de registrar
    private suspend fun registrar(context: Context, cercas: List<CercaEntity>): EstadoCercasAparelho {
        val cliente = LocationServices.getGeofencingClient(context)
        runCatching { cliente.removeGeofences(pendingIntent(context)).await() }
        if (cercas.isEmpty()) return EstadoCercasAparelho.SemCercas
        if (!temPermissao(context)) return EstadoCercasAparelho.SemPermissao
        val geofences = cercas.map { cerca ->
            Geofence.Builder()
                .setRequestId(idDaCerca(cerca))
                .setCircularRegion(cerca.latitude, cerca.longitude, cerca.raioM.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        // Sem gatilho inicial: já estar dentro de uma cerca ao abrir o app não gera alerta.
        val pedido = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()
        return try {
            cliente.addGeofences(pedido, pendingIntent(context)).await()
            EstadoCercasAparelho.Ativas(geofences.size)
        } catch (e: Exception) {
            EstadoCercasAparelho.Erro("Não foi possível ativar as cercas no GPS (localização desligada?).")
        }
    }

    /** "r:<remoteId>" ou "l:<id local>": o receiver acha a cerca no Room por esse id. */
    fun idDaCerca(cerca: CercaEntity) = cerca.remoteId?.let { "r:$it" } ?: "l:${cerca.id}"

    // O Play Services completa o intent com os dados da transição: precisa ser MUTABLE.
    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, GeofenceBroadcastReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
    )

    internal const val PREFS = "geofence_aparelho"
    internal const val CHAVE_PLACA = "placa"
    internal const val CHAVE_MOTORISTA = "motorista"
    internal const val CHAVE_UID = "uid"
}
