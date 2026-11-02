package br.com.rastreadorfrota.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.OrigemAlerta
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import br.com.rastreadorfrota.data.sync.SyncEngine
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recebe do Play Services as entradas e saídas das cercas REAIS (GPS do motorista), mesmo
 * com o app fechado. Segue a regra do projeto: grava primeiro no Room (alerta pendente) e a
 * sincronização envia para o Firestore depois, onde o gestor recebe.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) return
        val tipo = when (evento.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> TipoAlerta.ENTRADA
            Geofence.GEOFENCE_TRANSITION_EXIT -> TipoAlerta.SAIDA
            else -> return
        }
        val ids = evento.triggeringGeofences?.map { it.requestId }.orEmpty()
        val instante = evento.triggeringLocation?.time ?: System.currentTimeMillis()
        val prefs = context.getSharedPreferences(GeofenceAparelho.PREFS, Context.MODE_PRIVATE)
        val placa = prefs.getString(GeofenceAparelho.CHAVE_PLACA, null) ?: return
        val motorista = prefs.getString(GeofenceAparelho.CHAVE_MOTORISTA, null)
        val uid = prefs.getString(GeofenceAparelho.CHAVE_UID, null)

        // goAsync: o receiver ganha alguns segundos para gravar no banco fora da thread principal.
        val pendente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context)
                ids.forEach { id ->
                    val cerca = when {
                        id.startsWith("r:") -> dao.cercaDao().buscarPorRemoteId(id.removePrefix("r:"))
                        id.startsWith("l:") -> id.removePrefix("l:").toLongOrNull()?.let { dao.cercaDao().buscarPorId(it) }
                        else -> null
                    } ?: return@forEach
                    if (tipo == TipoAlerta.ENTRADA && !cerca.notificarEntrada) return@forEach
                    if (tipo == TipoAlerta.SAIDA && !cerca.notificarSaida) return@forEach
                    val alerta = AlertaEntity(
                        id = "GPS-${uid ?: placa}-$id-${tipo.name}-$instante",
                        cercaNome = cerca.nome,
                        placa = placa,
                        tipo = tipo.name,
                        origem = OrigemAlerta.APARELHO.name,
                        instante = instante,
                        motoristaNome = motorista,
                        uid = uid,
                        sincronizado = false // sobe para o Firestore na próxima sincronização
                    )
                    if (dao.alertaDao().inserir(alerta) != -1L) NotificacaoAlertas.notificar(context, alerta)
                }
                SyncEngine.sincronizarAgora()
            } finally {
                pendente.finish()
            }
        }
    }
}
