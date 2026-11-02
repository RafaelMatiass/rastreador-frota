package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.local.entity.AlvoCerca
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.rota.formatarDistancia
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.DialogoConfirmacao
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.iconeMarcador
import br.com.rastreadorfrota.ui.components.rememberMapaOsm
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.CercaViewModel
import br.com.rastreadorfrota.ui.viewmodel.PontoRota
import androidx.core.graphics.ColorUtils
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon

private val RAIOS = 50f..2000f

/**
 * Editor de cerca: o centro é o meio do mapa (o usuário arrasta o mapa até o lugar),
 * o raio vem do slider e o círculo é redesenhado ao vivo. Dá para buscar um endereço.
 */
@Composable
fun CercaEditorScreen(
    cercaId: Long?,
    onBack: () -> Unit,
    viewModel: CercaViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    var original by remember { mutableStateOf<CercaEntity?>(null) }
    var nome by remember { mutableStateOf("") }
    var raio by remember { mutableFloatStateOf(200f) }
    var alvo by remember { mutableStateOf(AlvoCerca.TODOS) }
    var tipoAlvo by remember { mutableStateOf(TipoVeiculo.CAMINHAO.name) }
    var placas by remember { mutableStateOf(setOf<String>()) }
    var entrada by remember { mutableStateOf(true) }
    var saida by remember { mutableStateOf(true) }
    var erroNome by remember { mutableStateOf<String?>(null) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var buscando by remember { mutableStateOf(false) }
    var centroLat by remember { mutableDoubleStateOf(CENTRO_DISTRIBUICAO.latitude) }
    var centroLon by remember { mutableDoubleStateOf(CENTRO_DISTRIBUICAO.longitude) }

    val mapa = rememberMapaOsm()
    val corCerca = MaterialTheme.colorScheme.tertiary.toArgb()
    val corCd = TrakSyncTheme.colors.textSecondary.toArgb()
    val iconeCd = remember(corCd) { iconeMarcador(mapa, corCd) }
    val circulo = remember { Polygon(mapa).apply { setOnClickListener { _, _, _ -> false } } }

    remember {
        mapa.overlays.add(circulo)
        mapa.overlays.add(Marker(mapa).apply {
            position = CENTRO_DISTRIBUICAO
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = NOME_CD
            icon = iconeCd
        })
        mapa.controller.setZoom(16.0)
        mapa.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                centroLat = mapa.mapCenter.latitude
                centroLon = mapa.mapCenter.longitude
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean = false
        })
        true
    }

    LaunchedEffect(cercaId) {
        val c = cercaId?.let { viewModel.carregar(it) } ?: return@LaunchedEffect
        original = c
        nome = c.nome
        raio = c.raioM.toFloat().coerceIn(RAIOS)
        alvo = c.alvoEnum
        if (c.alvoEnum == AlvoCerca.TIPO) tipoAlvo = c.alvoValor
        placas = c.placasAlvo
        entrada = c.notificarEntrada
        saida = c.notificarSaida
        mapa.controller.setCenter(GeoPoint(c.latitude, c.longitude))
        centroLat = c.latitude
        centroLon = c.longitude
    }

    // Círculo acompanha o centro do mapa e o raio do slider.
    LaunchedEffect(centroLat, centroLon, raio, corCerca) {
        circulo.points = Polygon.pointsAsCircle(GeoPoint(centroLat, centroLon), raio.toDouble())
        circulo.fillPaint.color = ColorUtils.setAlphaComponent(corCerca, 50)
        circulo.outlinePaint.color = corCerca
        circulo.outlinePaint.strokeWidth = 5f
        mapa.invalidate()
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { mapa },
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Mapa: arraste para posicionar o centro da cerca" }
            )
            Icon(
                Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.align(Alignment.Center).size(36.dp)
            )
            Row(
                Modifier
                    .statusBarsPadding()
                    .padding(Espaco.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Text("Buscar endereço do cliente", color = TrakSyncTheme.colors.textSecondary)
                    }
                }
                if (original != null) {
                    Spacer(Modifier.width(Espaco.sm))
                    Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, shadowElevation = 4.dp) {
                        IconButton(onClick = { confirmarExclusao = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir cerca", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Column(
                Modifier.align(Alignment.BottomEnd).padding(Espaco.md),
                verticalArrangement = Arrangement.spacedBy(Espaco.sm)
            ) {
                SmallFloatingActionButton(onClick = { mapa.controller.setCenter(CENTRO_DISTRIBUICAO) }, containerColor = MaterialTheme.colorScheme.surface) {
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

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp, shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0), bottomEnd = androidx.compose.foundation.shape.CornerSize(0))) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(Espaco.tela),
                verticalArrangement = Arrangement.spacedBy(Espaco.md)
            ) {
                Text(if (original == null) "Nova cerca" else "Editar cerca", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it; erroNome = null },
                    label = { Text("Nome da área") },
                    placeholder = { Text("Ex.: Supermercado Central") },
                    isError = erroNome != null,
                    supportingText = erroNome?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Raio", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(formatarDistancia(raio.toDouble()), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Slider(
                        value = raio,
                        onValueChange = { raio = (it / 10).toInt() * 10f },
                        valueRange = RAIOS,
                        modifier = Modifier.semantics { stateDescription = formatarDistancia(raio.toDouble()) }
                    )
                }

                Text("Vale para", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AlvoCerca.entries.forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = alvo == a,
                            onClick = { alvo = a },
                            shape = SegmentedButtonDefaults.itemShape(i, AlvoCerca.entries.size)
                        ) { Text(when (a) { AlvoCerca.TODOS -> "Todos"; AlvoCerca.TIPO -> "Por tipo"; AlvoCerca.VEICULOS -> "Escolher" }) }
                    }
                }
                val coresChip = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                )
                when (alvo) {
                    AlvoCerca.TIPO -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                        TipoVeiculo.entries.forEach { t ->
                            FilterChip(
                                selected = tipoAlvo == t.name,
                                onClick = { tipoAlvo = t.name },
                                label = { Text(t.label) },
                                leadingIcon = { Icon(t.icone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = coresChip
                            )
                        }
                    }
                    AlvoCerca.VEICULOS -> Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                        if (veiculos.isEmpty()) Text("Nenhum veículo aprovado na frota.", color = TrakSyncTheme.colors.textSecondary)
                        veiculos.forEach { v ->
                            FilterChip(
                                selected = v.placa in placas,
                                onClick = { placas = if (v.placa in placas) placas - v.placa else placas + v.placa },
                                label = { Text(v.placa) },
                                colors = coresChip
                            )
                        }
                    }
                    AlvoCerca.TODOS -> Text("Todos os veículos da frota geram alertas nesta área.", style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                }

                LinhaInterruptor("Avisar quando entrar", entrada) { entrada = it }
                LinhaInterruptor("Avisar quando sair", saida) { saida = it }

                BotaoPrincipal(
                    "Salvar cerca",
                    onClick = {
                        if (nome.isBlank()) {
                            erroNome = "Dê um nome para reconhecer a área nos alertas."
                            return@BotaoPrincipal
                        }
                        val base = original ?: CercaEntity(nome = "", latitude = 0.0, longitude = 0.0, raioM = 0.0)
                        viewModel.salvar(
                            base.copy(
                                nome = nome.trim(),
                                latitude = centroLat,
                                longitude = centroLon,
                                raioM = raio.toDouble(),
                                alvo = alvo.name,
                                alvoValor = when (alvo) {
                                    AlvoCerca.TODOS -> ""
                                    AlvoCerca.TIPO -> tipoAlvo
                                    AlvoCerca.VEICULOS -> placas.joinToString(",")
                                },
                                notificarEntrada = entrada,
                                notificarSaida = saida
                            )
                        )
                        onBack()
                    },
                    enabled = alvo != AlvoCerca.VEICULOS || placas.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (buscando) {
        PlanejadorRota(
            origem = PontoRota.Fixo(GeoPoint(centroLat, centroLon), "Centro atual da cerca"),
            destino = null,
            campoInicial = CampoRota.DESTINO,
            frota = emptyList(),
            entregaAtual = null,
            onEscolher = { _, ponto ->
                (ponto as? PontoRota.Fixo)?.let {
                    mapa.controller.animateTo(it.ponto, 17.0, 500L)
                    if (nome.isBlank()) nome = it.descricao
                }
                buscando = false
            },
            onEscolherNoMapa = { buscando = false },
            onInverter = {},
            onFechar = { buscando = false }
        )
    }
    if (confirmarExclusao) {
        DialogoConfirmacao(
            titulo = "Excluir \"${original?.nome}\"?",
            mensagem = "Os alertas desta área param em todos os aparelhos. O histórico já registrado continua.",
            confirmar = "Excluir",
            destrutivo = true,
            onConfirmar = {
                confirmarExclusao = false
                original?.let(viewModel::remover)
                onBack()
            },
            onCancelar = { confirmarExclusao = false }
        )
    }
}

@Composable
private fun LinhaInterruptor(texto: String, valor: Boolean, onMudar: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = onMudar, modifier = Modifier.semantics { contentDescription = texto })
    }
}

