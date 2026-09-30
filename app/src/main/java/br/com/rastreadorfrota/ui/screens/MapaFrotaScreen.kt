package br.com.rastreadorfrota.ui.screens

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.simulacao.PontoTelemetria
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.MapaViewModel
import br.com.rastreadorfrota.ui.viewmodel.VeiculoNoMapa
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val CENTRO_ARARAQUARA = GeoPoint(-21.7946, -48.1756)

/**
 * Mapa com a posição simulada dos veículos.
 * - Controlador: frota inteira, com filtro por status (parado / em trânsito).
 * - Motorista ([modoMotorista]): só o veículo associado a ele ([veiculoRemoteId]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapaFrotaScreen(
    onBack: () -> Unit,
    modoMotorista: Boolean = false,
    veiculoRemoteId: String? = null,
    viewModel: MapaViewModel = viewModel()
) {
    val frotaCompleta by viewModel.frota.collectAsState()
    var filtro by rememberSaveable { mutableStateOf<StatusVeiculo?>(null) }
    var selecionadoId by rememberSaveable { mutableStateOf<Long?>(null) }

    val frotaVisivel = frotaCompleta?.let { lista ->
        if (modoMotorista) lista.filter { veiculoRemoteId != null && it.veiculo.remoteId == veiculoRemoteId }
        else lista
    }
    val frotaFiltrada = frotaVisivel.orEmpty().filter { filtro == null || it.telemetria.status == filtro }
    val selecionado = frotaVisivel?.find { it.veiculo.id == selecionadoId }
        ?: frotaVisivel?.singleOrNull()?.takeIf { modoMotorista }

    val context = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(CENTRO_ARARAQUARA)
        }
    }
    // Um marcador por veículo, criado uma vez: a cada passo só mudam posição, ícone e texto.
    val marcadores = remember { mutableMapOf<Long, Marker>() }
    val corTransito = TrakSyncTheme.colors.success.toArgb()
    val corParado = TrakSyncTheme.colors.warning.toArgb()
    val icones = remember(corTransito, corParado) {
        mapOf(
            StatusVeiculo.EM_TRANSITO to iconeMarcador(mapView, corTransito),
            StatusVeiculo.PARADO to iconeMarcador(mapView, corParado)
        )
    }
    val aoTocarMarcador by rememberUpdatedState<(Long) -> Unit>({ selecionadoId = it })

    // O MapView precisa acompanhar o ciclo de vida da tela (tiles, threads, cache).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    // Enquadra a frota uma vez, quando os veículos chegam do Room.
    var enquadrou by remember { mutableStateOf(false) }
    LaunchedEffect(frotaVisivel?.isNotEmpty()) {
        if (!enquadrou && !frotaVisivel.isNullOrEmpty()) {
            enquadrar(mapView, frotaVisivel.map { it.telemetria })
            enquadrou = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (modoMotorista) "Meu veículo no mapa" else "Mapa da frota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (!frotaVisivel.isNullOrEmpty()) {
                        IconButton(onClick = {
                            selecionadoId = null
                            enquadrar(mapView, frotaVisivel.map { it.telemetria })
                        }) {
                            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Centralizar na frota")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (modoMotorista) 420.dp else 330.dp)
                    .clipToBounds(), // o MapView desenha fora dos limites sem isso
                update = { map ->
                    val visiveis = frotaFiltrada.associateBy { it.veiculo.id }
                    // Tira do mapa quem saiu (filtro, exclusão, inativação).
                    marcadores.keys.filter { it !in visiveis }.forEach { id ->
                        map.overlays.remove(marcadores.remove(id))
                    }
                    visiveis.forEach { (id, item) ->
                        val marker = marcadores.getOrPut(id) {
                            Marker(map).also {
                                it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                it.setOnMarkerClickListener { _, _ -> aoTocarMarcador(id); true }
                                map.overlays.add(it)
                            }
                        }
                        val t = item.telemetria
                        marker.position = GeoPoint(t.latitude, t.longitude)
                        marker.icon = icones.getValue(t.status)
                        marker.title = item.veiculo.placa
                    }
                    // Segue o veículo selecionado enquanto ele anda.
                    selecionado?.telemetria?.let { map.controller.animateTo(GeoPoint(it.latitude, it.longitude)) }
                    map.invalidate()
                }
            )

            when {
                frotaVisivel == null -> Carregando()
                modoMotorista && veiculoRemoteId == null -> EstadoVazio(
                    "Nenhum veículo atribuído",
                    "O controlador precisa associar um veículo a você."
                )
                modoMotorista && frotaVisivel.isEmpty() -> EstadoVazio(
                    "Veículo não encontrado",
                    "Seu veículo ainda não chegou neste aparelho. Sincronize e tente de novo."
                )
                frotaVisivel.isEmpty() -> EstadoVazio(
                    "Nenhum veículo cadastrado",
                    "Cadastre veículos para acompanhá-los no mapa."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selecionado != null) {
                        item(key = "painel") {
                            PainelTelemetria(
                                item = selecionado,
                                podeFechar = !modoMotorista,
                                onFechar = { selecionadoId = null }
                            )
                        }
                    }
                    if (!modoMotorista) {
                        item(key = "filtros") {
                            FiltrosStatus(frotaVisivel, filtro) { filtro = it }
                        }
                        items(frotaFiltrada, key = { it.veiculo.id }) { item ->
                            CardVeiculo(item, selecionado = item.veiculo.id == selecionado?.veiculo?.id) {
                                selecionadoId = item.veiculo.id
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconeMarcador(map: MapView, cor: Int): Drawable? =
    ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.marker_default)
        ?.mutate()
        ?.apply { setTint(cor) }

private fun enquadrar(map: MapView, pontos: List<PontoTelemetria>) {
    val geo = pontos.map { GeoPoint(it.latitude, it.longitude) }
    // zoomToBoundingBox só funciona depois que o mapa tem tamanho.
    map.post {
        if (geo.size == 1) {
            map.controller.setZoom(16.0)
            map.controller.animateTo(geo.first())
        } else if (geo.size > 1) {
            map.zoomToBoundingBox(BoundingBox.fromGeoPoints(geo).increaseByScale(1.3f), true)
        }
    }
}

@Composable
private fun corDoStatus(status: StatusVeiculo): Color = when (status) {
    StatusVeiculo.EM_TRANSITO -> TrakSyncTheme.colors.success
    StatusVeiculo.PARADO -> TrakSyncTheme.colors.warning
}

@Composable
private fun FiltrosStatus(
    frota: List<VeiculoNoMapa>,
    filtro: StatusVeiculo?,
    onFiltro: (StatusVeiculo?) -> Unit
) {
    val emTransito = frota.count { it.telemetria.status == StatusVeiculo.EM_TRANSITO }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filtro == null,
            onClick = { onFiltro(null) },
            label = { Text("Todos (${frota.size})") }
        )
        FilterChip(
            selected = filtro == StatusVeiculo.EM_TRANSITO,
            onClick = { onFiltro(StatusVeiculo.EM_TRANSITO) },
            label = { Text("Em trânsito ($emTransito)") }
        )
        FilterChip(
            selected = filtro == StatusVeiculo.PARADO,
            onClick = { onFiltro(StatusVeiculo.PARADO) },
            label = { Text("Parados (${frota.size - emTransito})") }
        )
    }
}

@Composable
private fun CardVeiculo(item: VeiculoNoMapa, selecionado: Boolean, onClick: () -> Unit) {
    val status = item.telemetria.status
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selecionado) MaterialTheme.colorScheme.primaryContainer
            else TrakSyncTheme.colors.surface2
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DirectionsCar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.veiculo.placa, style = MaterialTheme.typography.titleSmall)
                Text(item.veiculo.modelo, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = corDoStatus(status), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(status.label, style = MaterialTheme.typography.labelMedium, color = corDoStatus(status))
                }
                Text("${item.telemetria.velocidadeKmh} km/h", style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun PainelTelemetria(item: VeiculoNoMapa, podeFechar: Boolean, onFechar: () -> Unit) {
    val t = item.telemetria
    val v = item.veiculo
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // A foto só existe no aparelho em que o veículo foi fotografado.
                val foto = remember(v.fotoLocalPath) {
                    v.fotoLocalPath?.takeIf { File(it).exists() }?.let { BitmapFactory.decodeFile(it) }
                }
                if (foto != null) {
                    Image(
                        bitmap = foto.asImageBitmap(),
                        contentDescription = "Foto do veículo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(v.placa, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "${v.modelo} · ${TipoVeiculo.entries.find { it.name == v.tipo }?.label ?: v.tipo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrakSyncTheme.colors.textSecondary
                    )
                }
                if (podeFechar) {
                    IconButton(onClick = onFechar) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                ItemTelemetria("Status", t.status.label, corDoStatus(t.status))
                ItemTelemetria("Velocidade", "${t.velocidadeKmh} km/h")
                ItemTelemetria("Motor", if (t.motorLigado) "Ligado" else "Desligado")
                ItemTelemetria("Portas", if (t.portasAbertas) "Abertas" else "Fechadas")
            }
            Text(
                "Telemetria simulada · %.5f, %.5f".format(t.latitude, t.longitude),
                style = MaterialTheme.typography.labelSmall,
                color = TrakSyncTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
private fun ItemTelemetria(rotulo: String, valor: String, cor: Color = MaterialTheme.colorScheme.onBackground) {
    Column {
        Text(rotulo, style = MaterialTheme.typography.labelSmall, color = TrakSyncTheme.colors.textSecondary)
        Text(valor, style = MaterialTheme.typography.titleSmall, color = cor)
    }
}

@Composable
private fun Carregando() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EstadoVazio(titulo: String, mensagem: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            mensagem,
            style = MaterialTheme.typography.bodySmall,
            color = TrakSyncTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
