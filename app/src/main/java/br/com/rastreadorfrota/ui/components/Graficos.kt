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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Stepper da viagem no estilo dos apps de entrega: um trilho contínuo atrás e um marcador por
 * etapa (Ida → Entrega → Volta → Descarga). Etapas concluídas ganham um check, a atual fica
 * maior, com o ícone da etapa e um halo, e as próximas ficam em cinza.
 *
 * Todos os marcadores ficam centralizados numa faixa de altura fixa, então o trilho é sempre
 * uma linha reta, qualquer que seja o tamanho do marcador.
 *
 * [entregaCancelada]: a entrega foi cancelada e o veículo está voltando ao CD. A etapa
 * "Entrega" aparece com um X vermelho (não houve entrega), em vez do check.
 */
@Composable
fun StepperViagem(etapaAtual: EtapaViagem, modifier: Modifier = Modifier, entregaCancelada: Boolean = false) {
    val indiceAtual = ETAPAS_VIAGEM.indexOf(etapaAtual)
    val ativa = MaterialTheme.colorScheme.primary
    val noAtiva = MaterialTheme.colorScheme.onPrimary
    val inativa = TrakSyncTheme.colors.surface3
    val textoInativo = TrakSyncTheme.colors.textSecondary
    val erro = MaterialTheme.colorScheme.error
    val pulada = { i: Int -> entregaCancelada && ETAPAS_VIAGEM[i] == EtapaViagem.ENTREGA && i < indiceAtual }
    val n = ETAPAS_VIAGEM.size
    // Até onde o trilho fica pintado: do centro do 1º marcador ao centro do atual (0..1).
    val progresso by animateFloatAsState(
        targetValue = if (indiceAtual <= 0) 0f else indiceAtual / (n - 1f),
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progressoStepper"
    )
    Column(
        modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Etapa da viagem: ${etapaAtual.label}" + if (entregaCancelada) ", entrega cancelada" else ""
            }
    ) {
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val inicio = size.width / (2 * n)
                val fim = size.width - inicio
                val y = size.height / 2
                drawLine(inativa, Offset(inicio, y), Offset(fim, y), strokeWidth = size.height, cap = StrokeCap.Round)
                if (progresso > 0f) {
                    drawLine(ativa, Offset(inicio, y), Offset(inicio + (fim - inicio) * progresso, y), strokeWidth = size.height, cap = StrokeCap.Round)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ETAPAS_VIAGEM.forEachIndexed { i, etapa ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        when {
                            i == indiceAtual -> Box(
                                Modifier.size(36.dp).background(ativa.copy(alpha = 0.18f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(28.dp).background(ativa, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(etapa.icone, contentDescription = null, tint = noAtiva, modifier = Modifier.size(16.dp))
                                }
                            }
                            pulada(i) -> Box(Modifier.size(22.dp).background(erro, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(14.dp))
                            }
                            i < indiceAtual -> Box(Modifier.size(22.dp).background(ativa, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = noAtiva, modifier = Modifier.size(14.dp))
                            }
                            else -> Box(Modifier.size(22.dp).background(inativa, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(etapa.icone, contentDescription = null, tint = textoInativo, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            ETAPAS_VIAGEM.forEachIndexed { i, etapa ->
                Text(
                    if (pulada(i)) "Cancelada" else etapa.rotuloCurto,
                    style = if (i == indiceAtual) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    fontWeight = if (i == indiceAtual) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        i == indiceAtual -> ativa
                        pulada(i) -> erro
                        i < indiceAtual -> MaterialTheme.colorScheme.onSurface
                        else -> textoInativo
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Ícone de cada etapa no stepper. */
private val EtapaViagem.icone: ImageVector
    get() = when (this) {
        EtapaViagem.IDA -> Icons.Filled.LocalShipping
        EtapaViagem.ENTREGA -> Icons.Filled.Inventory2
        EtapaViagem.VOLTA -> Icons.Filled.KeyboardReturn
        EtapaViagem.DESCARGA -> Icons.Filled.Warehouse
        EtapaViagem.AGUARDANDO_ENTREGA, EtapaViagem.FORA_DE_OPERACAO -> Icons.Filled.LocalParking
    }
