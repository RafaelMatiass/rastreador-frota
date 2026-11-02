package br.com.rastreadorfrota.geofence

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.rastreadorfrota.MainActivity
import br.com.rastreadorfrota.R
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.OrigemAlerta
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Notificações Android de entrada e saída de cerca (Entrega 8). */
object NotificacaoAlertas {

    private const val CANAL = "alertas_cerca"

    /** Extra do intent: a placa do veículo, para o app abrir o mapa focado nele. */
    const val EXTRA_PLACA = "br.com.rastreadorfrota.extra.PLACA"

    fun criarCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(CANAL, "Alertas de cerca", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisa quando um veículo entra ou sai de uma área monitorada."
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Android 13+ exige a permissão POST_NOTIFICATIONS; antes disso basta não estarem bloqueadas. */
    fun temPermissao(context: Context): Boolean =
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun notificar(context: Context, alerta: AlertaEntity) {
        if (!temPermissao(context)) return
        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PLACA, alerta.placa)
        }
        val pendente = PendingIntent.getActivity(
            context,
            alerta.id.hashCode(),
            abrir,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val hora = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(alerta.instante))
        val titulo = when (alerta.tipoEnum) {
            TipoAlerta.ENTRADA -> "Chegada · ${alerta.cercaNome}"
            TipoAlerta.SAIDA -> "Saída · ${alerta.cercaNome}"
        }
        val quem = alerta.motoristaNome?.let { " (${it.substringBefore(" ")})" }.orEmpty()
        val texto = "${alerta.placa}$quem ${alerta.tipoEnum.verbo} ${alerta.cercaNome} às $hora" +
            if (alerta.origemEnum == OrigemAlerta.APARELHO) " · GPS do motorista" else ""

        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setColor(0xFF1677FF.toInt())
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendente)
            .setGroup(CANAL)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(alerta.id.hashCode(), notificacao)
        } catch (e: SecurityException) {
            // Permissão revogada entre a checagem e o envio: o alerta continua no histórico.
        }
    }
}
