package br.com.rastreadorfrota.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import coil.compose.SubcomposeAsyncImage
import java.io.File

/** Card padrão do app: superfície do tema, cantos grandes e borda sutil no tema claro. */
@Composable
fun CardTrak(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    destaque: Boolean = false,
    conteudo: @Composable () -> Unit
) {
    val cores = CardDefaults.cardColors(
        containerColor = if (destaque) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    )
    val borda = if (TrakSyncTheme.colors.isDark && !destaque) null
    else BorderStroke(1.dp, if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda) { conteudo() }
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = cores, border = borda) { conteudo() }
    }
}

/**
 * Selo de status: ícone + texto + cor. A informação nunca depende só da cor
 * (acessibilidade para daltônicos e leitores de tela).
 */
@Composable
fun StatusPill(texto: String, cor: Color, icone: ImageVector? = null, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(cor.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        } else {
            Box(Modifier.size(8.dp).background(cor, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = cor, maxLines = 1)
    }
}

/** Placa no estilo Mercosul: faixa azul em cima e letras grandes. */
@Composable
fun PlacaBadge(placa: String, modifier: Modifier = Modifier, compacta: Boolean = false) {
    val formatada = if (placa.length == 7) "${placa.take(3)}${if (placa[4].isLetter()) "" else "-"}${placa.drop(3)}" else placa
    Column(
        modifier = modifier
            .semantics { contentDescription = "Placa ${placa.toList().joinToString(" ")}" }
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFF1B2A3A), RoundedCornerShape(6.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = if (compacta) 4.dp else 6.dp)
                .background(Color(0xFF0B47A8))
        )
        Text(
            formatada,
            color = Color(0xFF111111),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (compacta) 13.sp else 16.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = if (compacta) 6.dp else 10.dp, vertical = if (compacta) 1.dp else 3.dp)
        )
    }
}

/** Número que conta até o valor novo: dá vida ao dashboard sem distrair. */
@Composable
fun NumeroAnimado(valor: Int, style: androidx.compose.ui.text.TextStyle, cor: Color = MaterialTheme.colorScheme.onSurface) {
    val animado by animateIntAsState(valor, animationSpec = tween(600), label = "numero")
    Text(animado.toString(), style = style, color = cor)
}

/** Indicador do dashboard: ícone, número animado e rótulo. Tocável quando leva a algum lugar. */
@Composable
fun KpiCard(
    icone: ImageVector,
    valor: Int,
    rotulo: String,
    cor: Color,
    modifier: Modifier = Modifier,
    sufixo: String = "",
    onClick: (() -> Unit)? = null
) {
    CardTrak(
        modifier = modifier.semantics(mergeDescendants = true) {},
        onClick = onClick
    ) {
        Column(Modifier.padding(Espaco.lg)) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(cor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(Espaco.md))
            Row(verticalAlignment = Alignment.Bottom) {
                NumeroAnimado(valor, MaterialTheme.typography.headlineSmall)
                if (sufixo.isNotEmpty()) {
                    Text(
                        sufixo,
                        style = MaterialTheme.typography.labelMedium,
                        color = TrakSyncTheme.colors.textSecondary,
                        modifier = Modifier.padding(start = 3.dp, bottom = 4.dp)
                    )
                }
            }
            Text(rotulo, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary, maxLines = 2)
        }
    }
}

/** Título de seção, marcado como cabeçalho para o TalkBack navegar por seções. */
@Composable
fun SecaoTitulo(
    titulo: String,
    modifier: Modifier = Modifier,
    acao: String? = null,
    onAcao: (() -> Unit)? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (acao != null && onAcao != null) {
            TextButton(onClick = onAcao) { Text(acao) }
        }
    }
}

/** Estado vazio: explica o que falta e oferece o próximo passo. */
@Composable
fun EstadoVazio(
    icone: ImageVector,
    titulo: String,
    mensagem: String,
    modifier: Modifier = Modifier,
    acao: String? = null,
    onAcao: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Espaco.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), TrakSyncTheme.colors.cyan.copy(alpha = 0.12f))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.size(Espaco.lg))
        Text(titulo, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.size(Espaco.xs))
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = TrakSyncTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
        if (acao != null && onAcao != null) {
            Spacer(Modifier.size(Espaco.lg))
            Button(onClick = onAcao, colors = trakSyncPrimaryButtonColors(), shape = MaterialTheme.shapes.medium) { Text(acao) }
        }
    }
}

/** Diálogo de confirmação para ações que não dá pra desfazer facilmente. */
@Composable
fun DialogoConfirmacao(
    titulo: String,
    mensagem: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    destrutivo: Boolean = false,
    icone: ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = icone?.let { { Icon(it, contentDescription = null) } },
        title = { Text(titulo) },
        text = { Text(mensagem) },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                colors = if (destrutivo) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else trakSyncPrimaryButtonColors()
            ) { Text(confirmar) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Bloco cinza com brilho passando: ocupa o lugar do conteúdo enquanto ele carrega. */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, cantos: Dp = 14.dp) {
    val transicao = rememberInfiniteTransition(label = "esqueleto")
    val deslocamento by transicao.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "brilho"
    )
    val base = TrakSyncTheme.colors.surface2
    val brilho = TrakSyncTheme.colors.surface3
    Box(
        modifier
            .clip(RoundedCornerShape(cantos))
            .background(
                Brush.linearGradient(
                    listOf(base, brilho, base),
                    start = Offset(deslocamento - 300f, 0f),
                    end = Offset(deslocamento, 0f)
                )
            )
    )
}

/**
 * Explica POR QUE o app precisa de uma permissão antes do diálogo do sistema
 * (pedido contextual, nunca na abertura do app).
 */
@Composable
fun AvisoPermissao(
    icone: ImageVector,
    titulo: String,
    mensagem: String,
    botao: String,
    onPermitir: () -> Unit,
    modifier: Modifier = Modifier,
    onDispensar: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Column(Modifier.padding(Espaco.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(Espaco.sm))
                Text(titulo, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.size(Espaco.xs))
            Text(mensagem, style = MaterialTheme.typography.bodySmall)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = Espaco.sm),
                horizontalArrangement = Arrangement.End
            ) {
                if (onDispensar != null) TextButton(onClick = onDispensar) { Text("Agora não") }
                Button(onClick = onPermitir, colors = trakSyncPrimaryButtonColors()) { Text(botao) }
            }
        }
    }
}

/** Foto do veículo (só existe no aparelho em que foi tirada); sem foto, o ícone do tipo. */
@Composable
fun FotoVeiculo(
    caminho: String?,
    icone: ImageVector = Icons.Filled.DirectionsCar,
    modifier: Modifier = Modifier,
    corFundo: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
) {
    val arquivo = caminho?.let(::File)?.takeIf { it.exists() }
    val reserva: @Composable () -> Unit = {
        Box(Modifier.background(corFundo).then(Modifier.fillMaxWidth()), contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(28.dp))
        }
    }
    Box(modifier.clip(MaterialTheme.shapes.medium).background(corFundo), contentAlignment = Alignment.Center) {
        if (arquivo != null) {
            SubcomposeAsyncImage(
                model = arquivo,
                contentDescription = "Foto do veículo",
                contentScale = ContentScale.Crop,
                error = { reserva() },
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
    }
}

/** Pequena informação com ícone (ex.: "45 km/h", "Portas fechadas"). */
@Composable
fun InfoChip(icone: ImageVector, texto: String, modifier: Modifier = Modifier, cor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(TrakSyncTheme.colors.surface2)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = cor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Botão secundário de altura confortável (mínimo 48 dp, cresce com a fonte do sistema). */
@Composable
fun BotaoSecundario(texto: String, icone: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icone != null) {
            Icon(icone, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Espaco.sm))
        }
        Text(texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Botão principal de altura confortável. */
@Composable
fun BotaoPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    enabled: Boolean = true,
    carregando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !carregando,
        shape = MaterialTheme.shapes.medium,
        colors = trakSyncPrimaryButtonColors(),
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        if (carregando) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            if (icone != null) {
                Icon(icone, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Espaco.sm))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Linha "rótulo: valor" usada nos detalhes. */
@Composable
fun LinhaDado(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Text(rotulo, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

@Composable
fun RowScope.Espacador() = Spacer(Modifier.weight(1f))

/** Semântica de botão para elementos clicáveis customizados. */
val PapelBotao = Role.Button
