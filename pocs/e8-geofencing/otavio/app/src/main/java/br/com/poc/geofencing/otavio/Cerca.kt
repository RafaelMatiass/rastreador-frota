package br.com.poc.geofencing.otavio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Cerca fixa da PoC: 200 m em volta do CD de Araraquara (o mesmo ponto do app). */
object Cerca {
    const val ID = "cd-araraquara"
    const val NOME = "CD Morada do Sol"
    const val LAT = -21.815086
    const val LON = -48.204879
    const val RAIO_M = 200f

    /** Distância em metros (haversine): é assim que o app confere as cercas da frota simulada. */
    fun distancia(lat1: Double, lon1: Double, lat2: Double = LAT, lon2: Double = LON): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * 6_371_000.0 * asin(sqrt(a))
    }
}

/** Notificação de entrada/saída, usada tanto pela cerca real quanto pela simulada. */
object Notificador {
    private const val CANAL = "cerca"

    fun notificar(context: Context, titulo: String, texto: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(CANAL, "Cercas", NotificationManager.IMPORTANCE_HIGH))
        }
        val n = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setAutoCancel(true)
            .build()
        try {
            nm.notify(System.currentTimeMillis().toInt(), n)
        } catch (e: SecurityException) {
            // Sem permissão de notificação: o evento continua aparecendo na tela.
        }
    }
}
