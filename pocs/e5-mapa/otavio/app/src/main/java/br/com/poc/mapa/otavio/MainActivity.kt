package br.com.poc.mapa.otavio

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val route = listOf(
    GeoPoint(-21.7946, -48.1756),
    GeoPoint(-21.8009, -48.1682),
    GeoPoint(-21.8072, -48.1604)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MapaPoc() } }
    }
}

@Composable
private fun MapaPoc() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            step = (step + 1) % route.size
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "PoC: mapa e posição simulada",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(20.dp)
        )
        Text(
            "O marcador muda de posição automaticamente.",
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(420.dp).padding(top = 16.dp),
            factory = { context ->
                Configuration.getInstance().userAgentValue = context.packageName
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(-21.8000, -48.1700))
                    setBackgroundColor(Color.DKGRAY)
                }
            },
            update = { map ->
                map.overlays.clear()
                map.overlays.add(Marker(map).apply {
                    position = route[step]
                    title = "Veículo demo"
                    snippet = if (step == route.lastIndex) "Parado - 0 km/h" else "Em trânsito - 42 km/h"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                map.invalidate()
            }
        )
        Text(
            "Posição ${step + 1}/${route.size} • ${if (step == route.lastIndex) "Parado" else "Em trânsito"}",
            modifier = Modifier.padding(20.dp)
        )
    }
}
