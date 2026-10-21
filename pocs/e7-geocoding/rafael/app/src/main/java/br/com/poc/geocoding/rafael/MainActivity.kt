package br.com.poc.geocoding.rafael

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private val ARARAQUARA = GeoPoint(-21.7946, -48.1756)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { TelaGeocoding() }
            }
        }
    }
}

@Composable
fun TelaGeocoding(vm: GeocodingViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(16.0)
            controller.setCenter(ARARAQUARA)
        }
    }
    val marcador = remember { Marker(mapView).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }
    remember {
        // Toque simples no mapa = "qual é o endereço deste ponto?"
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                p?.let(vm::buscar)
                return true
            }
            override fun longPressHelper(p: GeoPoint?) = false
        }))
        mapView.overlays.add(marcador)
    }

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
            modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),
            update = { map ->
                estado.ponto?.let { marcador.position = it }
                marcador.isEnabled = estado.ponto != null
                marcador.title = estado.endereco?.linha1
                map.invalidate()
            }
        )

        Column(Modifier.padding(16.dp)) {
            val ponto = estado.ponto
            if (ponto == null) {
                Text("Toque em qualquer lugar do mapa para ver o endereço.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text("Coordenada: %.5f, %.5f".format(ponto.latitude, ponto.longitude), style = MaterialTheme.typography.labelLarge)
            }
            when {
                estado.buscando -> Text("Consultando a Geoapify...", style = MaterialTheme.typography.bodyMedium)
                estado.erro != null -> Text(estado.erro!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                estado.endereco != null -> DetalhesEndereco(estado.endereco!!)
            }
            if (ponto != null && !estado.buscando && estado.erro == null) {
                Text(
                    if (estado.veioDoCache) "Fonte: cache (sem requisição)" else "Fonte: API Geoapify (${estado.tempoMs} ms)",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Text(
                "Requisições à API: ${estado.chamadasApi}  •  respostas do cache: ${estado.acertosCache}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun DetalhesEndereco(e: Endereco) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(e.linha1, style = MaterialTheme.typography.titleMedium)
        Text(e.linha2, style = MaterialTheme.typography.bodyMedium)
        Text(
            "Rua: ${e.rua ?: "-"} • Nº: ${e.numero ?: "-"} • Bairro: ${e.bairro ?: "-"}\n" +
                "Cidade: ${e.cidade ?: "-"} - ${e.estado ?: "-"} • CEP: ${e.cep ?: "-"}\n" +
                "Endereço encontrado a ${e.distanciaMetros.toInt()} m do ponto tocado",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
