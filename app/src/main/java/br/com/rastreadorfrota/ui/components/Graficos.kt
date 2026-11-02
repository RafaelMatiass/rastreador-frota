package br.com.rastreadorfrota.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

data class FatiaGrafico(val rotulo: String, val valor: Int, val cor: Color)

/**
 * Donut com o total no centro. Anima ao aparecer e quando os valores mudam.
 * A descrição para o TalkBack lista cada fatia (o gráfico não é só visual).
 */
@Composable
fun GraficoDonut(
    fatias: List<FatiaGrafico>,
    centroValor: String,
    centroRotulo: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 148.dp,
    espessura: Dp = 18.dp
) {
    val total = fatias.sumOf { it.valor }.coerceAtLeast(1)
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrada.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val trilho = TrakSyncTheme.colors.surface2
    val proporcoes = fatias.map { fatia ->
        animateFloatAsState(fatia.valor.toFloat() / total, tween(600), label = "fatia").value
    }
    Box(
        modifier
            .size(tamanho)
            .semantics {
                contentDescription = "$centroValor $centroRotulo. " + fatias.joinToString { "${it.rotulo}: ${it.valor}" }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(tamanho)) {
            val traco = espessura.toPx()
            val area = Size(size.width - traco, size.height - traco)
            val canto = Offset(traco / 2, traco / 2)
            drawArc(trilho, 0f, 360f, false, canto, area, style = Stroke(traco))
            var inicio = -90f
            val folga = if (fatias.count { it.valor > 0 } > 1) 3f else 0f
            fatias.forEachIndexed { i, fatia ->
                val angulo = 360f * proporcoes[i] * entrada.value
                if (angulo > folga) {
                    drawArc(fatia.cor, inicio + folga / 2, angulo - folga, false, canto, area, style = Stroke(traco, cap = StrokeCap.Round))
                }
                inicio += angulo
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centroValor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(centroRotulo, style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
        }
    }
}

/** Barra horizontal empilhada (ex.: quantos veículos em cada etapa da viagem). */
@Composable
fun BarraEmpilhada(fatias: List<FatiaGrafico>, modifier: Modifier = Modifier) {
    val total = fatias.sumOf { it.valor }
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(TrakSyncTheme.colors.surface2)
        ) {
            if (total > 0) {
                fatias.filter { it.valor > 0 }.forEach { fatia ->
                    key(fatia.rotulo) {
                        val peso by animateFloatAsState(fatia.valor.toFloat(), tween(600), label = "peso")
                        Box(
                            Modifier
                                .weight(peso.coerceAtLeast(0.01f))
                                .height(14.dp)
                                .background(fatia.cor)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            fatias.forEach { Legenda(it.cor, it.rotulo, it.valor.toString()) }
        }
    }
}

@Composable
fun Legenda(cor: Color, rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(cor, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text("$rotulo ", style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
        Text(valor, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** Stepper da viagem: CD → Ida → Entrega → Volta → Descarga, com a etapa atual destacada. */
@Composable
fun StepperViagem(etapaAtual: EtapaViagem, modifier: Modifier = Modifier) {
    val indiceAtual = ETAPAS_VIAGEM.indexOf(etapaAtual)
    val ativa = MaterialTheme.colorScheme.primary
    val inativa = TrakSyncTheme.colors.surface3
    Row(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Etapa da viagem: ${etapaAtual.label}" },
        verticalAlignment = Alignment.Top
    ) {
        ETAPAS_VIAGEM.forEachIndexed { i, etapa ->
            val feito = indiceAtual >= 0 && i <= indiceAtual
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(3.dp).background(if (i == 0) Color.Transparent else if (feito) ativa else inativa))
                    Box(
                        Modifier
                            .size(if (i == indiceAtual) 14.dp else 10.dp)
                            .background(if (feito) ativa else inativa, CircleShape)
                    )
                    Box(
                        Modifier.weight(1f).height(3.dp)
                            .background(if (i == ETAPAS_VIAGEM.lastIndex) Color.Transparent else if (indiceAtual > i) ativa else inativa)
                    )
                }
                Text(
                    etapa.rotuloCurto,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == indiceAtual) MaterialTheme.colorScheme.primary else TrakSyncTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
