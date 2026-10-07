package br.com.poc.rotas.rafael

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) { TelaRotas() }
            }
        }
    }
}

@Composable
fun TelaRotas(vm: RotaViewModel = viewModel()) {
    val estado by vm.estado.collectAsState()
    val context = LocalContext.current

    // Pede a permissão ao abrir; se negar, a origem fica no centro de distribuição.
    val pedirPermissao = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { concedidas ->
        if (concedidas.values.any { it }) vm.usarMinhaLocalizacao() else vm.usarCentroDistribuicao()
    }
    LaunchedEffect(Unit) {
        pedirPermissao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(14.0)
            controller.setCenter(CENTRO_DISTRIBUICAO)
        }
    }
    // Overlays criados uma vez; a cada estado novo só mudam pontos, posição e visibilidade.
    val linhaRota = remember {
        Polyline(mapView).apply {
            outlinePaint.color = Color.rgb(22, 119, 255)
            outlinePaint.strokeWidth = 12f
        }
    }
    val marcadorOrigem = remember { Marker(mapView).apply { title = "Origem" } }
    val marcadorDestino = remember { Marker(mapView).apply { title = "Destino" } }
    remember {
        // Toque longo no mapa escolhe o destino. Fica no índice 0 para não roubar o toque dos marcadores.
        val eventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?) = false
            override fun longPressHelper(p: GeoPoint?): Boolean {
                p?.let(vm::definirDestino)
                return true
            }
        })
        mapView.overlays.add(0, eventos)
        mapView.overlays.addAll(listOf(linhaRota, marcadorOrigem, marcadorDestino))
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Enquadra a rota inteira quando chega uma nova.
    LaunchedEffect(estado.rota) {
        val pontos = estado.rota?.pontos ?: return@LaunchedEffect
        mapView.post { mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(pontos).increaseByScale(1.3f), true) }
    }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds(),
            update = { map ->
                linhaRota.setPoints(estado.rota?.pontos.orEmpty())
                estado.origem?.let { marcadorOrigem.position = it }
                marcadorOrigem.isEnabled = estado.origem != null
                estado.destino?.let { marcadorDestino.position = it }
                marcadorDestino.isEnabled = estado.destino != null
                map.invalidate()
            }
        )

        Column(Modifier.padding(16.dp)) {
            Text(
                when {
                    estado.origem == null -> "Origem: aguardando..."
                    estado.origemEhGps -> "Origem: minha localização (GPS)"
                    else -> "Origem: centro de distribuição"
                },
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                when {
                    estado.calculando -> "Calculando rota..."
                    estado.rota != null -> "%.1f km  •  %d min".format(
                        estado.rota!!.distanciaMetros / 1000, (estado.rota!!.duracaoSegundos / 60).toInt()
                    )
                    else -> "Toque e segure no mapa para escolher o destino."
                },
                style = MaterialTheme.typography.titleMedium
            )
            estado.mensagem?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    pedirPermissao.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("GPS") }
                OutlinedButton(onClick = vm::usarCentroDistribuicao) { Text("Centro dist.") }
                OutlinedButton(onClick = vm::limpar) { Text("Limpar") }
            }
        }
    }
}
