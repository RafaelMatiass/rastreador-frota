package br.com.rastreadorfrota.ui.components

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Polygon

/**
 * Estilo do mapa feito no próprio aparelho, sobre os tiles padrão do OpenStreetMap (Mapnik),
 * que são gratuitos e sem chave (o manual permite Google Maps ou OpenStreetMap).
 *
 * - Claro: cores mais suaves e fundo mais limpo, para os marcadores da frota se destacarem.
 * - Escuro: cores invertidas e puxadas para o azul-marinho da marca (modo noturno).
 */
private fun filtroDoTema(escuro: Boolean): ColorMatrixColorFilter {
    val matriz = ColorMatrix().apply { setSaturation(if (escuro) 0.25f else 0.55f) }
    if (escuro) {
        matriz.postConcat(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        // Fundo quase preto vira o azul-marinho do app; textos claros continuam legíveis.
        matriz.postConcat(
            ColorMatrix(
                floatArrayOf(
                    0.88f, 0f, 0f, 0f, 10f,
                    0f, 0.90f, 0f, 0f, 20f,
                    0f, 0f, 0.92f, 0f, 36f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    } else {
        // Leve clareada: o fundo fica quase branco e as ruas, bem definidas.
        matriz.postConcat(
            ColorMatrix(
                floatArrayOf(
                    0.94f, 0f, 0f, 0f, 14f,
                    0f, 0.94f, 0f, 0f, 14f,
                    0f, 0f, 0.94f, 0f, 18f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    return ColorMatrixColorFilter(matriz)
}

private fun aplicarEstilo(mapa: MapView, escuro: Boolean, fundo: Int) {
    mapa.overlayManager.tilesOverlay.apply {
        setColorFilter(filtroDoTema(escuro))
        // Enquanto o tile carrega, mostra a cor de fundo do app (sem a grade cinza padrão).
        loadingBackgroundColor = fundo
        loadingLineColor = fundo
    }
    mapa.invalidate()
}

/**
 * MapView do osmdroid já configurado para o app: tiles do OpenStreetMap com estilo conforme o
 * tema, zoom só por gesto e pelos botões do próprio app, limites de zoom e ruas legíveis em
 * telas de alta densidade. O ciclo de vida (onResume/onPause/onDetach) acompanha a tela.
 */
@Composable
fun rememberMapaOsm(): MapView {
    val context = LocalContext.current
    val escuro = TrakSyncTheme.colors.isDark
    val fundo = MaterialTheme.colorScheme.background.toArgb()
    val mapa = remember { criarMapa(context) }
    // Também troca na hora se o tema do aparelho mudar com a tela aberta.
    LaunchedEffect(escuro, fundo) { aplicarEstilo(mapa, escuro, fundo) }

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

private fun criarMapa(context: Context) = MapView(context).apply {
    setTileSource(TileSourceFactory.MAPNIK)
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
