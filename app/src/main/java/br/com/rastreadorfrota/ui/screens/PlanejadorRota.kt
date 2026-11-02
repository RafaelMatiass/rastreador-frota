package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.BuscaEnderecoViewModel
import br.com.rastreadorfrota.ui.viewmodel.PontoRota

enum class CampoRota { ORIGEM, DESTINO }

/**
 * Planejador estilo Waze/Google Maps: dois campos (origem e destino) com busca digitada,
 * atalhos (minha localização, CD, entrega atual, veículos, recentes) e "escolher no mapa".
 */
@Composable
fun PlanejadorRota(
    origem: PontoRota,
    destino: PontoRota?,
    campoInicial: CampoRota,
    frota: List<VeiculoNoMapa>,
    entregaAtual: PontoRota.Fixo?,
    onEscolher: (CampoRota, PontoRota) -> Unit,
    onEscolherNoMapa: (CampoRota) -> Unit,
    onInverter: () -> Unit,
    onFechar: () -> Unit,
    busca: BuscaEnderecoViewModel = viewModel()
) {
    val estado by busca.estado.collectAsState()
    var campo by remember { mutableStateOf(campoInicial) }
    val foco = LocalFocusManager.current
    val focoOrigem = remember { FocusRequester() }
    val focoDestino = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        busca.limpar()
        (if (campoInicial == CampoRota.ORIGEM) focoOrigem else focoDestino).requestFocus()
    }

    fun escolher(ponto: PontoRota) {
        onEscolher(campo, ponto)
        busca.limpar()
        // Escolheu a origem e o destino ainda está vazio: o foco vai para o destino.
        if (campo == CampoRota.ORIGEM && destino == null) {
            campo = CampoRota.DESTINO
            focoDestino.requestFocus()
        } else {
            foco.clearFocus()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(Modifier.padding(start = 4.dp, end = Espaco.sm, top = Espaco.sm), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onFechar) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar ao mapa") }
                Column(Modifier.weight(1f)) {
                    CampoPonto(
                        rotulo = "Origem",
                        icone = Icons.Filled.TripOrigin,
                        corIcone = MaterialTheme.colorScheme.primary,
                        escolhido = origem.descricao,
                        ativo = campo == CampoRota.ORIGEM,
                        texto = if (campo == CampoRota.ORIGEM) estado.texto else "",
                        onTexto = { busca.digitar(it) },
                        onFoco = { if (it) { campo = CampoRota.ORIGEM; busca.limpar() } },
                        focusRequester = focoOrigem
                    )
                    Spacer(Modifier.size(6.dp))
                    CampoPonto(
                        rotulo = "Destino",
                        icone = Icons.Filled.LocationOn,
                        corIcone = MaterialTheme.colorScheme.error,
                        escolhido = destino?.descricao,
                        ativo = campo == CampoRota.DESTINO,
                        texto = if (campo == CampoRota.DESTINO) estado.texto else "",
                        onTexto = { busca.digitar(it) },
                        onFoco = { if (it) { campo = CampoRota.DESTINO; busca.limpar() } },
                        focusRequester = focoDestino
                    )
                }
                IconButton(onClick = onInverter, enabled = destino != null) {
                    Icon(Icons.Filled.SwapVert, contentDescription = "Inverter origem e destino")
                }
            }
            if (estado.buscando) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = Espaco.sm))
            else Spacer(Modifier.size(Espaco.sm + 4.dp))

            LazyColumn(Modifier.fillMaxSize()) {
                if (estado.texto.trim().length >= 3) {
                    items(estado.resultados, key = { "r" + it.titulo + it.subtitulo }) { lugar ->
                        Opcao(Icons.Filled.Place, lugar.titulo, lugar.subtitulo) {
                            busca.lembrar(lugar)
                            escolher(PontoRota.Fixo(lugar.ponto, lugar.titulo))
                        }
                    }
                    estado.erro?.let { erro ->
                        item { Aviso(Icons.Filled.WifiOff, erro) }
                    }
                    if (estado.semResultados) {
                        item { Aviso(Icons.Filled.SearchOff, "Nenhum endereço encontrado. Tente incluir o número ou o bairro.") }
                    }
                } else {
                    item {
                        Opcao(Icons.Filled.MyLocation, "Minha localização", "GPS do aparelho") { escolher(PontoRota.MinhaLocalizacao) }
                    }
                    item {
                        Opcao(Icons.Filled.Warehouse, NOME_CD, "Centro de distribuição") {
                            escolher(PontoRota.Fixo(CENTRO_DISTRIBUICAO, NOME_CD))
                        }
                    }
                    if (entregaAtual != null) {
                        item { Opcao(Icons.Filled.Storefront, entregaAtual.descricao, "Entrega atual") { escolher(entregaAtual) } }
                    }
                    item {
                        Opcao(Icons.Filled.TouchApp, "Escolher no mapa", "Arraste o mapa até o ponto desejado") { onEscolherNoMapa(campo) }
                    }
                    if (estado.recentes.isNotEmpty()) {
                        item { Cabecalho("Recentes") }
                        items(estado.recentes, key = { "h" + it.titulo + it.subtitulo }) { lugar ->
                            Opcao(Icons.Filled.History, lugar.titulo, lugar.subtitulo) { escolher(PontoRota.Fixo(lugar.ponto, lugar.titulo)) }
                        }
                    }
                    if (frota.isNotEmpty()) {
                        item { Cabecalho("Veículos da frota") }
                        items(frota, key = { "v" + it.veiculo.id }) { item ->
                            Opcao(
                                tipoVeiculo(item.veiculo.tipo).icone,
                                item.veiculo.placa,
                                item.motorista?.nome ?: item.veiculo.modelo
                            ) {
                                escolher(PontoRota.Fixo(item.posicao, "Veículo ${item.veiculo.placa}", item.veiculo.id))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoPonto(
    rotulo: String,
    icone: ImageVector,
    corIcone: Color,
    escolhido: String?,
    ativo: Boolean,
    texto: String,
    onTexto: (String) -> Unit,
    onFoco: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        placeholder = {
            Text(
                if (ativo && escolhido != null) "Buscar outro endereço..." else escolhido ?: "Buscar endereço ou local",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (escolhido != null && !ativo) MaterialTheme.colorScheme.onSurface else TrakSyncTheme.colors.textSecondary
            )
        },
        label = { Text(rotulo) },
        leadingIcon = { Icon(icone, contentDescription = null, tint = corIcone) },
        trailingIcon = {
            if (texto.isNotEmpty()) IconButton(onClick = { onTexto("") }) { Icon(Icons.Filled.Clear, contentDescription = "Limpar busca") }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {}),
        shape = MaterialTheme.shapes.medium,
        colors = trakSyncTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { onFoco(it.isFocused) }
    )
}

@Composable
private fun Opcao(icone: ImageVector, titulo: String, subtitulo: String, onClick: () -> Unit) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = Espaco.tela, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .background(TrakSyncTheme.colors.surface2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Espaco.md))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitulo.isNotBlank()) {
                    Text(subtitulo, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = 70.dp), color = TrakSyncTheme.colors.surface2)
    }
}

@Composable
private fun Cabecalho(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = TrakSyncTheme.colors.textSecondary,
        modifier = Modifier.padding(start = Espaco.tela, top = Espaco.lg, bottom = Espaco.xs)
    )
}

@Composable
private fun Aviso(icone: ImageVector, texto: String) {
    Row(Modifier.padding(Espaco.tela), verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, contentDescription = null, tint = TrakSyncTheme.colors.textSecondary)
        Spacer(Modifier.width(Espaco.md))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)
    }
}
