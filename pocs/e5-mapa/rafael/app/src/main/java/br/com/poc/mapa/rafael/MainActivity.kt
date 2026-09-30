package br.com.poc.mapa.rafael

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val VERDE = 0xFF2E9E5B.toInt()
private const val AMBAR = 0xFFE3A21A.toInt()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { TelaSimulador() }
            }
        }
    }
}

@Composable
fun TelaSimulador(vm: SimuladorViewModel = viewModel()) {
    val veiculos by vm.veiculos.collectAsState()
    val pausado by vm.pausado.collectAsState()
    val tick by vm.tick.collectAsState()
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(-21.7880, -48.1720))
        }
    }
    // Um marcador por placa, criado UMA vez; a cada passo só muda posição/ícone/texto.
    val marcadores = remember { mutableMapOf<String, Marker>() }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(), // o MapView desenha fora dos limites sem isso
            update = { map ->
                veiculos.forEach { v ->
                    val marker = marcadores.getOrPut(v.placa) {
                        Marker(map).also {
                            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            it.title = v.placa
                            map.overlays.add(it)
                        }
                    }
                    val p = v.ponto
                    marker.position = GeoPoint(p.lat, p.lon)
                    marker.icon = ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.marker_default)
                        ?.mutate()?.apply { setTint(if (p.emTransito) VERDE else AMBAR) }
                    marker.snippet = descricao(p)
                    // O balão aberto não se atualiza sozinho: reabre para mostrar a telemetria nova.
                    if (marker.isInfoWindowShown) marker.showInfoWindow()
                }
                map.invalidate()
            }
        )

        Column(Modifier.padding(16.dp)) {
            Text("Passo $tick  •  intervalo ${SimuladorViewModel.INTERVALO_MS / 1000}s", style = MaterialTheme.typography.labelLarge)
            veiculos.forEach { v ->
                Text("${v.placa}: ${descricao(v.ponto)}", style = MaterialTheme.typography.bodyMedium)
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = vm::alternarPausa) { Text(if (pausado) "Retomar" else "Pausar") }
            }
        }
    }
}

private fun descricao(p: PontoTelemetria): String =
    (if (p.emTransito) "Em trânsito" else "Parado") +
        " • ${p.velocidadeKmh} km/h" +
        " • motor ${if (p.motorLigado) "ligado" else "desligado"}" +
        " • portas ${if (p.portasAbertas) "abertas" else "fechadas"}"
