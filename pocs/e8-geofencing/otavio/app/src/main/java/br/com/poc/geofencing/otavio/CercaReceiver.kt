package br.com.poc.geofencing.otavio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Recebe do Play Services as transições da cerca REAL, mesmo com o app fechado. */
class CercaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val evento = GeofencingEvent.fromIntent(intent) ?: return
        if (evento.hasError()) return
        val tipo = when (evento.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "Entrou em"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "Saiu de"
            else -> return
        }
        Eventos.adicionar("[GPS real] $tipo ${Cerca.NOME}")
        Notificador.notificar(context, "$tipo ${Cerca.NOME}", "Transição detectada pelo GeofencingClient")
    }
}
