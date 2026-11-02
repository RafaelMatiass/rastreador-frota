package br.com.rastreadorfrota.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Polygon

private val SERVIDORES_CARTO = arrayOf("a", "b", "c", "d")

/** Tiles claros (CARTO Voyager) e escuros (CARTO Dark Matter), ambos com dados do OpenStreetMap. */
private val TILES_CLARO: ITileSource = XYTileSource(
    "CartoVoyager", 0, 20, 256, ".png",
    SERVIDORES_CARTO.map { "https://$it.basemaps.cartocdn.com/rastertiles/voyager/" }.toTypedArray(),
    "© OpenStreetMap contributors © CARTO"
)
private val TILES_ESCURO: ITileSource = XYTileSource(
    "CartoDarkMatter", 0, 20, 256, ".png",
    SERVIDORES_CARTO.map { "https://$it.basemaps.cartocdn.com/dark_all/" }.toTypedArray(),
    "© OpenStreetMap contributors © CARTO"
)

/**
 * MapView do osmdroid já configurado para o app: tiles conforme o tema, zoom só por gesto e
 * pelos botões do próprio app, limites de zoom e ruas legíveis em telas de alta densidade.
 * O ciclo de vida (onResume/onPause/onDetach) acompanha a tela.
 */
@Composable
fun rememberMapaOsm(): MapView {
    val context = LocalContext.current
    val escuro = TrakSyncTheme.colors.isDark
    val mapa = remember { criarMapa(context, escuro) }
    // Tema do aparelho mudou com a tela aberta: troca os tiles.
    LaunchedEffect(escuro) {
        val desejado = if (escuro) TILES_ESCURO else TILES_CLARO
        if (mapa.tileProvider.tileSource.name() != desejado.name()) mapa.setTileSource(desejado)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> mapa.onResume()
                Lifecycle.Event.ON_PAUSE -> mapa.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapa.onDetach()
        }
    }
    return mapa
}

private fun criarMapa(context: Context, escuro: Boolean) = MapView(context).apply {
    setTileSource(if (escuro) TILES_ESCURO else TILES_CLARO)
    setMultiTouchControls(true)
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    isTilesScaledToDpi = true
    minZoomLevel = 5.0
    maxZoomLevel = 19.5
    isVerticalMapRepetitionEnabled = false
    controller.setZoom(13.0)
    controller.setCenter(CENTRO_DISTRIBUICAO)
    overlays.add(CopyrightOverlay(context).apply { setTextSize(9) })
}

/** Enquadra os pontos. [margemInferiorPx] deixa espaço para o painel que cobre a parte de baixo. */
fun enquadrar(map: MapView, geo: List<GeoPoint>, margemInferiorPx: Int = 0, zoomUnico: Double = 16.0) {
    if (geo.isEmpty()) return
    // zoomToBoundingBox só funciona depois que o mapa tem tamanho.
    map.post {
        if (geo.size == 1 || geo.distinct().size == 1) {
            map.controller.animateTo(geo.first(), maxOf(map.zoomLevelDouble, zoomUnico), 600L)
        } else {
            val caixa = BoundingBox.fromGeoPoints(geo)
            val borda = (48 * map.resources.displayMetrics.density).toInt()
            map.zoomToBoundingBox(caixa, true, borda, map.maxZoomLevel - 1.5, 600L)
            if (margemInferiorPx > 0) map.post { map.scrollBy(0, margemInferiorPx / 2) }
        }
    }
}

/** Círculo translúcido de uma cerca (geofence). */
fun circuloCerca(map: MapView, cerca: CercaEntity, cor: Int, destaque: Boolean = false): Polygon =
    Polygon(map).apply {
        points = Polygon.pointsAsCircle(GeoPoint(cerca.latitude, cerca.longitude), cerca.raioM)
        fillPaint.color = ColorUtils.setAlphaComponent(cor, if (destaque) 70 else 38)
        outlinePaint.color = ColorUtils.setAlphaComponent(cor, 200)
        outlinePaint.strokeWidth = if (destaque) 5f else 3f
        title = cerca.nome
        setOnClickListener { _, _, _ -> false } // não rouba o toque dos marcadores
    }

fun iconeMarcador(map: MapView, cor: Int): Drawable? =
    ContextCompat.getDrawable(map.context, org.osmdroid.library.R.drawable.marker_default)
        ?.mutate()
        ?.apply { setTint(cor) }
