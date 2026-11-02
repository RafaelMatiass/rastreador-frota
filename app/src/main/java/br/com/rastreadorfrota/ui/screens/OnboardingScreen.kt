package br.com.rastreadorfrota.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import kotlinx.coroutines.launch

private data class PaginaIntro(
    val icone: ImageVector,
    val satelites: List<ImageVector>,
    val titulo: String,
    val texto: String
)

private val PAGINAS = listOf(
    PaginaIntro(
        Icons.Filled.Map,
        listOf(Icons.Filled.LocalShipping, Icons.Filled.Place, Icons.Filled.Storefront),
        "Toda a frota em um mapa",
        "Veja onde cada caminhão, furgão e van da distribuidora está agora, o que está entregando e quanto falta para chegar ao cliente."
    ),
    PaginaIntro(
        Icons.Filled.Radar,
        listOf(Icons.Filled.NotificationsActive, Icons.Filled.Place, Icons.Filled.LocalShipping),
        "Alertas na hora certa",
        "Crie cercas no CD e nos clientes. O app avisa quando um veículo entra ou sai de cada área, pelo GPS real ou pela telemetria."
    ),
    PaginaIntro(
        Icons.Filled.Route,
        listOf(Icons.Filled.Directions, Icons.Filled.Storefront, Icons.Filled.Map),
        "Rotas e entregas na palma da mão",
        "Digite um endereço ou escolha no mapa: rota por ruas, passo a passo e atalho para Waze ou Google Maps. Funciona até sem internet para cadastrar."
    )
)

/** Introdução de 3 páginas, mostrada só na primeira abertura do app. */
@Composable
fun OnboardingScreen(onConcluir: () -> Unit) {
    val pager = rememberPagerState { PAGINAS.size }
    val escopo = rememberCoroutineScope()
    val ultima = pager.currentPage == PAGINAS.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Espaco.tela, vertical = Espaco.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Marca()
                Spacer(Modifier.weight(1f))
                if (!ultima) TextButton(onClick = onConcluir) { Text("Pular") }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { indice ->
                val pagina = PAGINAS[indice]
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = Espaco.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Ilustracao(pagina)
                    Spacer(Modifier.height(48.dp))
                    Text(
                        pagina.titulo,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Espaco.md))
                    Text(
                        pagina.texto,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TrakSyncTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(Espaco.tela),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier.semantics { contentDescription = "Página ${pager.currentPage + 1} de ${PAGINAS.size}" },
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(PAGINAS.size) { i ->
                        val largura by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, label = "indicador")
                        val cor by animateColorAsState(
                            if (i == pager.currentPage) MaterialTheme.colorScheme.primary else TrakSyncTheme.colors.surface3,
                            label = "cor"
                        )
                        Box(Modifier.height(8.dp).width(largura).background(cor, CircleShape))
                    }
                }
                Spacer(Modifier.weight(1f))
                BotaoPrincipal(
                    texto = if (ultima) "Começar" else "Próximo",
                    icone = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        if (ultima) onConcluir() else escopo.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    }
                )
            }
        }
    }
}

/** Ícone central pulsando e três "satélites" girando devagar em volta. */
@Composable
private fun Ilustracao(pagina: PaginaIntro) {
    val infinita = rememberInfiniteTransition(label = "intro")
    val pulso by infinita.animateFloat(1f, 1.08f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulso")
    val giro by infinita.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000)), label = "giro")
    val primaria = MaterialTheme.colorScheme.primary
    val ciano = TrakSyncTheme.colors.cyan
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(220.dp).background(primaria.copy(alpha = 0.06f), CircleShape))
        Box(Modifier.size(160.dp).background(primaria.copy(alpha = 0.10f), CircleShape))
        Box(Modifier.size(240.dp).rotate(giro)) {
            pagina.satelites.forEachIndexed { i, icone ->
                val angulo = Math.toRadians(i * 120.0)
                Satelite(
                    icone,
                    Modifier
                        .align(Alignment.Center)
                        .offset(x = (96 * kotlin.math.cos(angulo)).dp, y = (96 * kotlin.math.sin(angulo)).dp)
                        .rotate(-giro),
                    if (i % 2 == 0) ciano else primaria
                )
            }
        }
        Box(
            Modifier
                .scale(pulso)
                .size(104.dp)
                .background(Brush.linearGradient(listOf(primaria, ciano)), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(pagina.icone, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
    }
}

@Composable
private fun Satelite(icone: ImageVector, modifier: Modifier, cor: Color, tamanho: Dp = 44.dp) {
    Box(
        modifier
            .size(tamanho)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(2.dp)
            .background(cor.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(22.dp))
    }
}

/** Logotipo: TRAK + SYNC (ciano). */
@Composable
fun Marca(modifier: Modifier = Modifier) {
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "TrakSync" }, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(30.dp)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, TrakSyncTheme.colors.cyan)), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text("TRAK", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Text("SYNC", style = MaterialTheme.typography.titleLarge, color = TrakSyncTheme.colors.cyan)
    }
}
