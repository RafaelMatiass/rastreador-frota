package br.com.rastreadorfrota.ui.components

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirportShuttle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlin.math.roundToInt

/** Símbolo de cada tipo de veículo, usado no marcador do mapa e na lista. */
val TipoVeiculo.icone: ImageVector
    get() = when (this) {
        TipoVeiculo.CAMINHAO -> Icons.Filled.LocalShipping
        TipoVeiculo.FURGAO -> Icons.Filled.AirportShuttle
        TipoVeiculo.VAN -> Icons.Filled.DirectionsBus
        TipoVeiculo.OUTRO -> Icons.Filled.DirectionsCar
    }

/** O tipo é gravado como texto no Room; um valor desconhecido vira [TipoVeiculo.OUTRO]. */
fun tipoVeiculo(nome: String): TipoVeiculo =
    TipoVeiculo.entries.find { it.name == nome } ?: TipoVeiculo.OUTRO

/**
 * Marcadores do mapa: círculo na cor do status, borda branca e o símbolo do tipo no centro.
 * O osmdroid desenha `Drawable`, não Compose, então cada combinação tipo × status é
 * desenhada uma vez num bitmap e reaproveitada a cada passo da simulação.
 */
@Composable
fun rememberIconesVeiculo(coresPorStatus: Map<StatusVeiculo, Color>): Map<Pair<TipoVeiculo, StatusVeiculo>, Drawable> {
    val context = LocalContext.current
    val density = LocalDensity.current
    val simbolos = TipoVeiculo.entries.associateWith { rememberVectorPainter(it.icone) }
    return remember(coresPorStatus, density) {
        buildMap {
            simbolos.forEach { (tipo, simbolo) ->
                coresPorStatus.forEach { (status, cor) -> put(tipo to status, desenhar(context, density, simbolo, cor)) }
            }
        }
    }
}

private fun desenhar(context: android.content.Context, density: Density, simbolo: Painter, cor: Color): Drawable {
    val lado = with(density) { 36.dp.toPx() }
    val bitmap = ImageBitmap(lado.roundToInt(), lado.roundToInt())
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(bitmap), Size(lado, lado)) {
        val raio = size.minDimension / 2
        drawCircle(Color.Black.copy(alpha = 0.25f), raio)          // contorno suave, destaca do mapa
        drawCircle(Color.White, raio - 1.dp.toPx())
        drawCircle(cor, raio - 3.dp.toPx())
        val ladoSimbolo = size.minDimension * 0.55f
        translate((size.width - ladoSimbolo) / 2, (size.height - ladoSimbolo) / 2) {
            with(simbolo) { draw(Size(ladoSimbolo, ladoSimbolo), colorFilter = ColorFilter.tint(Color.White)) }
        }
    }
    return BitmapDrawable(context.resources, bitmap.asAndroidBitmap())
}
