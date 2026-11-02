package br.com.poc.geofencing.otavio

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Log de eventos visível na tela (alimentado pela cerca real e pela simulada). */
object Eventos {
    val lista = mutableStateListOf<String>()
    fun adicionar(texto: String) {
        lista.add(0, "${SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())}  $texto")
    }
}

/**
 * PoC da Entrega 8 (geofencing), duas abordagens:
 * 1. Cerca REAL: GeofencingClient do Google com o GPS do aparelho (teste no emulador mudando a
 *    localização em Extended Controls > Location, para dentro e para fora do CD).
 * 2. Cerca SIMULADA: um veículo "anda" de 0 a 1000 m do CD pelo slider; a cada movimento a
 *    distância (haversine) é comparada com o raio, e a troca dentro/fora vira notificação.
 *    É a mesma lógica que o app usa com a telemetria simulada da frota.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { Tela() }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun Tela() {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var status by remember { mutableStateOf("Cerca real não registrada") }
    var distanciaSimulada by remember { mutableFloatStateOf(0f) }
    var dentroSimulado by remember { mutableStateOf(true) }

    fun temPermissao() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)

    val pendingIntent = remember {
        PendingIntent.getBroadcast(
            context, 0, Intent(context, CercaReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )
    }

    fun registrar() {
        if (!temPermissao()) { status = "Falta permissão de localização \"o tempo todo\""; return }
        val cerca = Geofence.Builder()
            .setRequestId(Cerca.ID)
            .setCircularRegion(Cerca.LAT, Cerca.LON, Cerca.RAIO_M)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val pedido = GeofencingRequest.Builder().setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER).addGeofence(cerca).build()
        escopo.launch {
            status = try {
                LocationServices.getGeofencingClient(context).addGeofences(pedido, pendingIntent).await()
                "Cerca real ativa: ${Cerca.NOME}, raio ${Cerca.RAIO_M.toInt()} m"
            } catch (e: Exception) {
                "Erro ao registrar: ${e.message}"
            }
        }
    }

    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { registrar() }
    val pedirPermissoes = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else registrar()
    }

    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PoC Geofencing (Entrega 8)", style = MaterialTheme.typography.headlineSmall)

        Text("1. Cerca real (GPS)", style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = {
            val p = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) p += Manifest.permission.POST_NOTIFICATIONS
            pedirPermissoes.launch(p.toTypedArray())
        }, modifier = Modifier.fillMaxWidth()) { Text("Pedir permissões e registrar cerca") }
        OutlinedButton(onClick = {
            LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent)
            status = "Cerca real removida"
        }, modifier = Modifier.fillMaxWidth()) { Text("Remover cerca real") }

        Text("2. Cerca simulada (telemetria)", style = MaterialTheme.typography.titleMedium)
        Text("Veículo a ${distanciaSimulada.toInt()} m do CD · ${if (dentroSimulado) "dentro" else "fora"} da cerca")
        Slider(
            value = distanciaSimulada,
            onValueChange = { metros ->
                distanciaSimulada = metros
                // Converte a distância num ponto ao norte do CD e confere pela haversine.
                val lat = Cerca.LAT + metros / 111_320.0
                val dentro = Cerca.distancia(lat, Cerca.LON) <= Cerca.RAIO_M
                if (dentro != dentroSimulado) {
                    dentroSimulado = dentro
                    val tipo = if (dentro) "Entrou em" else "Saiu de"
                    Eventos.adicionar("[Simulado] ABC1D23 $tipo ${Cerca.NOME}")
                    Notificador.notificar(context, "$tipo ${Cerca.NOME}", "ABC1D23 · telemetria simulada")
                }
            },
            valueRange = 0f..1000f
        )

        Text("Eventos", style = MaterialTheme.typography.titleMedium)
        LazyColumn { items(Eventos.lista) { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp)) } }
    }
}
