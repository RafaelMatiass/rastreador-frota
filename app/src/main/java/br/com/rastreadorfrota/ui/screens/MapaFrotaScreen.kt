package br.com.rastreadorfrota.ui.screens

import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import kotlinx.coroutines.delay
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private data class SimulatedLocation(
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val speed: String
)

private val simulatedRoute = listOf(
    SimulatedLocation(-21.7946, -48.1756, "Em trânsito", "42 km/h"),
    SimulatedLocation(-21.8009, -48.1682, "Em trânsito", "36 km/h"),
    SimulatedLocation(-21.8072, -48.1604, "Parado", "0 km/h")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaFrotaScreen(
    onBack: () -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsState()
    var simulationStep by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(4_000)
            simulationStep = (simulationStep + 1) % simulatedRoute.size
        }
    }

    val fleet = if (veiculos.isEmpty()) {
        listOf(
            VeiculoEntity(id = -1, placa = "ABC1D23", modelo = "Frota demo", tipo = "CAMINHAO"),
            VeiculoEntity(id = -2, placa = "XYZ4E56", modelo = "Frota demo", tipo = "FURGAO")
        )
    } else {
        veiculos
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mapa da frota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapContext ->
                        Configuration.getInstance().userAgentValue = mapContext.packageName
                        MapView(mapContext).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                            controller.setCenter(GeoPoint(-21.8000, -48.1700))
                            setBackgroundColor(Color.rgb(7, 17, 31))
                        }
                    },
                    update = { map ->
                        map.overlays.clear()
                        fleet.forEachIndexed { index, vehicle ->
                            val route = simulatedRoute[(simulationStep + index) % simulatedRoute.size]
                            map.overlays.add(
                                Marker(map).apply {
                                    position = GeoPoint(route.latitude, route.longitude)
                                    title = vehicle.placa
                                    snippet = "${vehicle.modelo} • ${route.status} • ${route.speed}"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                }
                            )
                        }
                        map.invalidate()
                    }
                )
            }

            Text(
                "Monitoramento em tempo real",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
            Text(
                if (veiculos.isEmpty()) "Modo demonstração com posições simuladas"
                else "Posições simuladas para ${veiculos.size} veículo(s)",
                style = MaterialTheme.typography.bodySmall,
                color = TrakSyncTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fleet, key = { it.id }) { vehicle ->
                    val route = simulatedRoute[(simulationStep + fleet.indexOf(vehicle)) % simulatedRoute.size]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(vehicle.placa, style = MaterialTheme.typography.titleSmall)
                                Text(vehicle.modelo, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = statusColor(route.status), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(route.status, style = MaterialTheme.typography.labelMedium, color = statusColor(route.status))
                                }
                                Text(route.speed, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusColor(status: String): ComposeColor =
    if (status == "Em trânsito") TrakSyncTheme.colors.success else TrakSyncTheme.colors.warning