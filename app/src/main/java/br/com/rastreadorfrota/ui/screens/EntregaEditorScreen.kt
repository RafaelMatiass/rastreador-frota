package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.simulacao.encadearEntrega
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.iconeMarcador
import br.com.rastreadorfrota.ui.components.rememberMapaOsm
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.EntregaViewModel
import br.com.rastreadorfrota.ui.viewmodel.EstadoNovaEntrega
import br.com.rastreadorfrota.ui.viewmodel.PontoRota
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

/**
 * Gestor atribui uma entrega ao veículo: o destino é o centro do mapa (arrastar ou buscar
 * o endereço). Ao confirmar, a rota é gerada no OSRM e o veículo sai do CD no próximo passo.
 */
@Composable
fun EntregaEditorScreen(
    veiculoId: Long,
    onBack: () -> Unit,
    onCriada: (Long) -> Unit,
    viewModel: EntregaViewModel = viewModel(),
    veiculoViewModel: VeiculoViewModel = viewModel()
) {
    val veiculo by remember(veiculoId) { veiculoViewModel.observar(veiculoId) }.collectAsStateWithLifecycle(initialValue = null)
    val motoristas by veiculoViewModel.motoristas.collectAsStateWithLifecycle()
    val criacao by viewModel.criacao.collectAsStateWithLifecycle()
    val motorista = motoristas.find { it.ativo && it.veiculoId == veiculoId }
    val entregasDoVeiculo by remember(veiculo?.placa) { viewModel.doVeiculo(veiculo?.placa.orEmpty()) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    // Onde e quando esta entrega vai começar (CD agora, ou depois da última da fila).
    val partida = remember(entregasDoVeiculo) {
        val agora = System.currentTimeMillis()
        encadearEntrega(entregasDoVeiculo.filter { it.ativa(agora) }, agora)
    }

    var cliente by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf<String?>(null) }
    // Ponto a que o endereço da busca se refere: se o mapa sair dali, o endereço deixa de valer.
    var pontoEndereco by remember { mutableStateOf<GeoPoint?>(null) }
    var comDesvio by remember { mutableStateOf(false) }
    var erroCliente by remember { mutableStateOf<String?>(null) }
    var buscando by remember { mutableStateOf(false) }
    var destinoLat by remember { mutableDoubleStateOf(CENTRO_DISTRIBUICAO.latitude) }
    var destinoLon by remember { mutableDoubleStateOf(CENTRO_DISTRIBUICAO.longitude) }

    val mapa = rememberMapaOsm()
    val corCd = TrakSyncTheme.colors.textSecondary.toArgb()
    val iconeCd = remember(corCd) { iconeMarcador(mapa, corCd) }
    remember {
        mapa.overlays.add(Marker(mapa).apply {
            position = CENTRO_DISTRIBUICAO
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = NOME_CD
            icon = iconeCd
        })
        mapa.controller.setZoom(14.0)
        mapa.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                destinoLat = mapa.mapCenter.latitude
                destinoLon = mapa.mapCenter.longitude
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean = false
        })
        true
    }

    // O endereço da busca só vale enquanto o centro do mapa estiver nele; se o gestor arrastar
    // o mapa para outro lugar, o endereço é calculado de novo ao salvar.
    val enderecoAtual = endereco?.takeIf {
        pontoEndereco?.let { p -> p.distanceToAsDouble(GeoPoint(destinoLat, destinoLon)) <= 50 } == true
    }

    LaunchedEffect(criacao) {
        (criacao as? EstadoNovaEntrega.Criada)?.let { onCriada(it.id) }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { mapa },
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Mapa: arraste para posicionar o destino da entrega no centro" }
            )
            Icon(
                Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp).size(40.dp)
            )
            Row(Modifier.statusBarsPadding().padding(Espaco.md), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                }
                Spacer(Modifier.width(Espaco.sm))
                Surface(
                    onClick = { buscando = true },
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Row(Modifier.padding(horizontal = Espaco.lg, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Espaco.sm))
                        Text(enderecoAtual ?: "Buscar endereço do cliente", color = TrakSyncTheme.colors.textSecondary, maxLines = 1)
                    }
                }
            }
            Column(Modifier.align(Alignment.BottomEnd).padding(Espaco.md), verticalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                SmallFloatingActionButton(onClick = { mapa.controller.animateTo(CENTRO_DISTRIBUICAO) }, containerColor = MaterialTheme.colorScheme.surface) {
                    Icon(Icons.Filled.Warehouse, contentDescription = "Centralizar no CD")
                }
                SmallFloatingActionButton(onClick = { mapa.controller.zoomIn() }, containerColor = MaterialTheme.colorScheme.surface) {
                    Icon(Icons.Filled.Add, contentDescription = "Aproximar")
                }
                SmallFloatingActionButton(onClick = { mapa.controller.zoomOut() }, containerColor = MaterialTheme.colorScheme.surface) {
                    Icon(Icons.Filled.Remove, contentDescription = "Afastar")
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = CornerSize(0), bottomEnd = CornerSize(0))
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(Espaco.tela),
                verticalArrangement = Arrangement.spacedBy(Espaco.md)
            ) {
                Text("Nova entrega", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(Espaco.sm))
                    Text(
                        "${veiculo?.placa ?: "…"} · ${motorista?.nome ?: "sem motorista ativo"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    if (partida.fimDaAnterior == null) "Sai de ${partida.origemNome} assim que confirmar."
                    else "Entra na fila: sai de ${partida.origemNome} às " +
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(partida.inicio)) +
                        ", sem voltar ao CD antes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TrakSyncTheme.colors.textSecondary
                )
                OutlinedTextField(
                    value = cliente,
                    onValueChange = { cliente = it; erroCliente = null },
                    label = { Text("Cliente") },
                    placeholder = { Text("Ex.: Supermercado Central") },
                    isError = erroCliente != null,
                    supportingText = erroCliente?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Destino: ${enderecoAtual ?: "centro do mapa (%.5f, %.5f)".format(destinoLat, destinoLon)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TrakSyncTheme.colors.textSecondary
                )
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Simular desvio de rota", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Demonstração: no meio da ida o veículo sai do trajeto planejado e o app gera o alerta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TrakSyncTheme.colors.textSecondary
                        )
                    }
                    Switch(checked = comDesvio, onCheckedChange = { comDesvio = it }, modifier = Modifier.semantics { contentDescription = "Simular desvio de rota" })
                }
                (criacao as? EstadoNovaEntrega.Erro)?.let {
                    Text(it.mensagem, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                BotaoPrincipal(
                    "Atribuir entrega",
                    onClick = {
                        val v = veiculo ?: return@BotaoPrincipal
                        if (cliente.isBlank()) {
                            erroCliente = "Informe o cliente que recebe a entrega."
                            return@BotaoPrincipal
                        }
                        val destino = GeoPoint(destinoLat, destinoLon)
                        if (destino.distanceToAsDouble(CENTRO_DISTRIBUICAO) < 300) {
                            erroCliente = "Arraste o mapa até o endereço do cliente (o destino está no CD)."
                            return@BotaoPrincipal
                        }
                        viewModel.limparErro()
                        viewModel.criar(v, motorista?.nome, cliente.trim(), destino, enderecoAtual, comDesvio)
                    },
                    enabled = veiculo != null && motorista != null,
                    carregando = criacao == EstadoNovaEntrega.Gerando,
                    modifier = Modifier.fillMaxWidth()
                )
                if (motorista == null) {
                    Text(
                        "Atribua um motorista ativo ao veículo antes de criar a entrega.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (buscando) {
        PlanejadorRota(
            origem = PontoRota.Fixo(CENTRO_DISTRIBUICAO, NOME_CD),
            destino = null,
            campoInicial = CampoRota.DESTINO,
            frota = emptyList(),
            entregaAtual = null,
            onEscolher = { _, ponto ->
                (ponto as? PontoRota.Fixo)?.let {
                    pontoEndereco = it.ponto
                    endereco = it.descricao
                    destinoLat = it.ponto.latitude
                    destinoLon = it.ponto.longitude
                    mapa.controller.setZoom(17.0)
                    mapa.controller.setCenter(it.ponto)
                    if (cliente.isBlank()) cliente = it.descricao.substringBefore(",")
                }
                buscando = false
            },
            onEscolherNoMapa = { buscando = false },
            onInverter = {},
            onFechar = { buscando = false }
        )
    }
}
