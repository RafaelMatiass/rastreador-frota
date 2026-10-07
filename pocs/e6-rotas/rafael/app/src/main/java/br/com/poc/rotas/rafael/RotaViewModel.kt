package br.com.poc.rotas.rafael

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.osmdroid.util.GeoPoint

/** Centro de distribuição fictício em Araraquara: origem alternativa para o emulador. */
val CENTRO_DISTRIBUICAO = GeoPoint(-21.7946, -48.1756)

data class EstadoRota(
    val origem: GeoPoint? = null,
    val origemEhGps: Boolean = false,
    val destino: GeoPoint? = null,
    val rota: Rota? = null,
    val calculando: Boolean = false,
    val mensagem: String? = null,
    // Simulação de um veículo percorrendo a rota
    val veiculo: GeoPoint? = null,
    val trajetoRestante: List<GeoPoint> = emptyList(),
    val faltamMetros: Double = 0.0,
    val faltamSegundos: Long = 0
)

/**
 * Fluxo da PoC: origem (GPS ou ponto fixo) + destino (toque longo no mapa)
 * -> pede a rota ao OSRM -> a tela desenha a polyline.
 */
class RotaViewModel(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoRota())
    val estado: StateFlow<EstadoRota> = _estado.asStateFlow()

    private val localizacao = LocationServices.getFusedLocationProviderClient(application)
    private var calculo: Job? = null
    private var simulacao: Job? = null

    /** Só deve ser chamada depois que a permissão de localização foi concedida. */
    @SuppressLint("MissingPermission")
    fun usarMinhaLocalizacao() {
        viewModelScope.launch {
            _estado.update { it.copy(mensagem = "Obtendo localização...") }
            val local = runCatching {
                // getCurrentLocation pede uma leitura nova; lastLocation é o plano B (pode ser antiga).
                localizacao.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                    ?: localizacao.lastLocation.await()
            }.getOrNull()
            if (local == null) {
                _estado.update { it.copy(mensagem = "Localização indisponível. Use o centro de distribuição.") }
            } else {
                _estado.update { it.copy(origem = GeoPoint(local.latitude, local.longitude), origemEhGps = true, mensagem = null) }
                recalcular()
            }
        }
    }

    fun usarCentroDistribuicao() {
        _estado.update { it.copy(origem = CENTRO_DISTRIBUICAO, origemEhGps = false, mensagem = null) }
        recalcular()
    }

    fun definirDestino(ponto: GeoPoint) {
        _estado.update { it.copy(destino = ponto) }
        recalcular()
    }

    fun limpar() {
        calculo?.cancel()
        pararSimulacao()
        _estado.update { it.copy(destino = null, rota = null, calculando = false, mensagem = null) }
    }

    /**
     * Um veículo percorre a rota: a cada [INTERVALO_MS] avança [PASSO_M] metros SOBRE a linha,
     * e o trajeto restante encolhe. É a técnica usada na simulação da frota no app.
     */
    fun simularViagem() {
        val trajeto = _estado.value.rota?.pontos ?: return
        pararSimulacao()
        simulacao = viewModelScope.launch {
            val passos = amostrar(trajeto, PASSO_M)
            passos.forEachIndexed { i, atual ->
                val restante = trajetoRestante(trajeto, atual)
                _estado.update {
                    it.copy(
                        veiculo = atual.ponto,
                        trajetoRestante = restante,
                        faltamMetros = comprimentoMetros(restante),
                        faltamSegundos = (passos.size - 1 - i) * INTERVALO_MS / 1000
                    )
                }
                delay(INTERVALO_MS)
            }
        }
    }

    private fun pararSimulacao() {
        simulacao?.cancel()
        _estado.update { it.copy(veiculo = null, trajetoRestante = emptyList()) }
    }

    private fun recalcular() {
        val origem = _estado.value.origem ?: return
        val destino = _estado.value.destino ?: return
        pararSimulacao()
        calculo?.cancel() // um toque novo cancela o pedido anterior
        calculo = viewModelScope.launch {
            _estado.update { it.copy(calculando = true, mensagem = null) }
            val resultado = runCatching {
                OsrmClient.rota(origem, destino, getApplication<Application>().packageName)
            }
            // Pedido cancelado por um toque mais novo: não sobrescreve o estado do pedido novo.
            (resultado.exceptionOrNull() as? CancellationException)?.let { throw it }
            _estado.update {
                it.copy(
                    calculando = false,
                    rota = resultado.getOrNull(),
                    mensagem = resultado.exceptionOrNull()?.let { e -> "Erro: ${e.message}" }
                )
            }
        }
    }

    private companion object {
        const val INTERVALO_MS = 1_000L
        const val PASSO_M = 60.0 // acelerado para a demonstração (~216 km/h)
    }
}
