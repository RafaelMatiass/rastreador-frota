package br.com.rastreadorfrota.ui.screens

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.geocoding.Endereco
import br.com.rastreadorfrota.geocoding.GeocodingService
import br.com.rastreadorfrota.navigation.PedidosNavegacao
import br.com.rastreadorfrota.rota.PERMISSOES_LOCALIZACAO
import br.com.rastreadorfrota.rota.localizacaoAtual
import br.com.rastreadorfrota.rota.temPermissaoLocalizacao
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.circuloCerca
import br.com.rastreadorfrota.ui.components.corDoStatus
import br.com.rastreadorfrota.ui.components.enquadrar
import br.com.rastreadorfrota.ui.components.iconeMarcador
import br.com.rastreadorfrota.ui.components.rememberIconesVeiculo
import br.com.rastreadorfrota.ui.components.rememberMapaOsm
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.CameraMapa
import br.com.rastreadorfrota.ui.viewmodel.EnderecoViewModel
import br.com.rastreadorfrota.ui.viewmodel.MapaViewModel
import br.com.rastreadorfrota.ui.viewmodel.PontoRota
import br.com.rastreadorfrota.ui.viewmodel.RotaViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.math.abs

/**
 * Mapa da frota em tela cheia, com painel deslizante embaixo.
 * - Gestor: frota inteira, filtro por status, cercas e lista de veículos no painel.
 * - Motorista: só o veículo dele, com atalho para navegar até a entrega.
 *
 * Câmera: só se move sozinha por ação explícita (selecionar veículo, rota nova, botões).
 * Qualquer gesto do usuário (arrastar, pinçar) desliga o "seguir" e nada é escolhido
 * por toque longo: o destino da rota vem do planejador ("Para onde?").
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapaFrotaScreen(
    usuario: Usuario?,
    onAbrirVeiculo: (Long) -> Unit,
    viewModel: MapaViewModel = viewModel(),
    rotaViewModel: RotaViewModel = viewModel(),
    enderecoViewModel: EnderecoViewModel = viewModel()
) {
    val modoMotorista = usuario?.perfil == Perfil.MOTORISTA
    LaunchedEffect(modoMotorista) { viewModel.definirModo(modoMotorista) }
    val frotaCompleta by viewModel.frota.collectAsStateWithLifecycle()
    val cercas by viewModel.cercas.collectAsStateWithLifecycle()
    val estadoRota by rotaViewModel.estado.collectAsStateWithLifecycle()
    val enderecos by enderecoViewModel.estado.collectAsStateWithLifecycle()
    val pedido by PedidosNavegacao.mapa.collectAsStateWithLifecycle()

    val frotaVisivel = frotaCompleta?.let { lista ->
        if (modoMotorista) lista.filter { it.veiculo.remoteId != null && it.veiculo.remoteId == usuario?.veiculoRemoteId }
        else lista
    }
    val frotaFiltrada = frotaVisivel.orEmpty().filter { viewModel.filtro == null || it.status == viewModel.filtro }
    val selecionado = frotaVisivel?.find { it.veiculo.id == viewModel.selecionadoId }
        ?: frotaVisivel?.singleOrNull()?.takeIf { modoMotorista }

    val context = LocalContext.current
    val densidade = LocalDensity.current
    val escopo = rememberCoroutineScope()
    val sheet = rememberBottomSheetScaffoldState()
    var planejando by rememberSaveable { mutableStateOf<CampoRota?>(null) }
    var escolhendoNoMapa by rememberSaveable { mutableStateOf<CampoRota?>(null) }
    var temLocalizacao by remember { mutableStateOf(temPermissaoLocalizacao(context)) }

    val mapa = rememberMapaOsm()

    // Ações que precisam da localização pedem a permissão na hora (nunca na abertura).
    var acaoPendente by remember { mutableStateOf<(() -> Unit)?>(null) }
    val pedirPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        temLocalizacao = temPermissaoLocalizacao(context)
        acaoPendente?.invoke()
        acaoPendente = null
    }
    fun comLocalizacao(acao: () -> Unit) {
        if (temPermissaoLocalizacao(context)) acao()
        else {
            acaoPendente = acao
            pedirPermissao.launch(PERMISSOES_LOCALIZACAO)
        }
    }

    fun definirPonto(campo: CampoRota, ponto: PontoRota) {
        val aplicar = {
            if (campo == CampoRota.ORIGEM) rotaViewModel.definirOrigem(ponto) else rotaViewModel.definirDestino(ponto)
        }
        // "Minha localização" em qualquer ponta precisa do GPS.
        val usaGps = ponto == PontoRota.MinhaLocalizacao ||
            (campo == CampoRota.DESTINO && estadoRota.origemEscolhida == PontoRota.MinhaLocalizacao)
        if (usaGps) comLocalizacao(aplicar) else aplicar()
    }

    val margemPainelPx = with(densidade) { 260.dp.roundToPx() }

    fun selecionar(item: VeiculoNoMapa) {
        viewModel.selecionadoId = item.veiculo.id
        viewModel.seguindo = true
        mapa.controller.animateTo(item.posicao, maxOf(mapa.zoomLevelDouble, 16.0), 600L)
        escopo.launch { sheet.bottomSheetState.partialExpand() }
    }

    fun destinoDoVeiculo(item: VeiculoNoMapa) = PontoRota.Fixo(item.posicao, "Veículo ${item.veiculo.placa}", item.veiculo.id)

    // ----- Cores e ícones -----
    val corViagem = MaterialTheme.colorScheme.primary.toArgb()
    val corEntrega = MaterialTheme.colorScheme.error.toArgb()
    val corRota = TrakSyncTheme.colors.cyan.toArgb()
    val corCd = TrakSyncTheme.colors.textSecondary.toArgb()
    val corCerca = MaterialTheme.colorScheme.tertiary.toArgb()
    val iconeOrigem = remember(corViagem) { iconeMarcador(mapa, corViagem) }
    val iconeDestino = remember(corEntrega) { iconeMarcador(mapa, corEntrega) }
    val iconeEntrega = remember(corEntrega) { iconeMarcador(mapa, corEntrega) }
    val iconeCd = remember(corCd) { iconeMarcador(mapa, corCd) }
    val icones = rememberIconesVeiculo(StatusVeiculo.entries.associateWith { corDoStatus(it) })
    val halo = remember(corViagem) {
        val lado = (60 * context.resources.displayMetrics.density).toInt()
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ColorUtils.setAlphaComponent(corViagem, 60))
            setStroke((3 * context.resources.displayMetrics.density).toInt(), corViagem)
            setSize(lado, lado)
        }
    }

    // ----- Overlays fixos, criados uma vez -----
    val linhaRota = remember {
        Polyline(mapa).apply {
            outlinePaint.strokeWidth = 14f
            outlinePaint.strokeCap = android.graphics.Paint.Cap.ROUND
        }
    }
    val marcadorOrigem = remember { Marker(mapa).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); title = "Origem da rota" } }
    val marcadorDestino = remember { Marker(mapa).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }
    val marcadorCd = remember {
        Marker(mapa).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            position = CENTRO_DISTRIBUICAO
            title = NOME_CD
        }
    }
    val marcadorEntrega = remember { Marker(mapa).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) } }
    val marcadorHalo = remember { Marker(mapa).apply { setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER); setInfoWindow(null) } }
    val minhaPosicao = remember { MyLocationNewOverlay(GpsMyLocationProvider(context), mapa) }
    val linhasViagem = remember { mutableMapOf<Long, Polyline>() }
    val marcadores = remember { mutableMapOf<Long, Marker>() }
    val circulosCercas = remember { mutableListOf<Polygon>() }
    val aoTocarMarcador by rememberUpdatedState<(Long) -> Unit>({ id -> frotaVisivel?.find { it.veiculo.id == id }?.let(::selecionar) })
    val aoTocarMapa by rememberUpdatedState<() -> Unit>({ if (!modoMotorista) viewModel.selecionadoId = null })
    val aoMexerNoMapa by rememberUpdatedState<() -> Unit>({ viewModel.seguindo = false })

    // Centro do mapa enquanto o usuário escolhe um ponto (atualizado quando o mapa para).
    var centroMapa by remember { mutableStateOf(mapa.mapCenter.let { GeoPoint(it.latitude, it.longitude) }) }

    remember {
        // Toque simples no vazio fecha o painel do veículo. Sem toque longo: pinçar não escolhe nada.
        mapa.overlays.add(0, MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean { aoTocarMapa(); return false }
            override fun longPressHelper(p: GeoPoint?) = false
        }))
        mapa.overlays.addAll(listOf(linhaRota, marcadorCd, marcadorEntrega, marcadorOrigem, marcadorDestino, marcadorHalo))
        // Gesto do usuário (arrasto além da tolerância, ou dois dedos) = ele está no controle.
        val tolerancia = ViewConfiguration.get(context).scaledTouchSlop
        var x0 = 0f
        var y0 = 0f
        mapa.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { x0 = ev.x; y0 = ev.y }
                MotionEvent.ACTION_POINTER_DOWN -> aoMexerNoMapa()
                MotionEvent.ACTION_MOVE -> if (abs(ev.x - x0) > tolerancia || abs(ev.y - y0) > tolerancia) aoMexerNoMapa()
            }
            false
        }
        mapa.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean {
                centroMapa = mapa.mapCenter.let { GeoPoint(it.latitude, it.longitude) }
                return false
            }
            override fun onZoom(event: ZoomEvent?): Boolean = false
        })
        // Volta para onde estava ao trocar de aba.
        viewModel.camera?.let { mapa.controller.setZoom(it.zoom); mapa.controller.setCenter(it.centro) }
        true
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.camera = CameraMapa(GeoPoint(mapa.mapCenter.latitude, mapa.mapCenter.longitude), mapa.zoomLevelDouble)
        }
    }

    // Ponto azul da localização do aparelho, quando há permissão.
    LaunchedEffect(temLocalizacao) {
        if (temLocalizacao && minhaPosicao !in mapa.overlays) {
            minhaPosicao.enableMyLocation()
            mapa.overlays.add(minhaPosicao)
        }
    }
    DisposableEffect(Unit) { onDispose { minhaPosicao.disableMyLocation() } }

    // Cercas desenhadas como círculos (recriadas só quando as cercas mudam).
    LaunchedEffect(cercas, viewModel.mostrarCercas, corCerca) {
        mapa.overlays.removeAll(circulosCercas)
        circulosCercas.clear()
        if (viewModel.mostrarCercas) {
            cercas.filter { it.ativa }.forEach { circulosCercas += circuloCerca(mapa, it, corCerca) }
            mapa.overlays.addAll(1, circulosCercas)
        }
        mapa.invalidate()
    }

    // Enquadra a frota uma vez, na primeira abertura.
    LaunchedEffect(frotaVisivel?.isNotEmpty()) {
        if (!viewModel.enquadrouFrota && viewModel.camera == null && !frotaVisivel.isNullOrEmpty()) {
            enquadrar(mapa, frotaVisivel.map { it.posicao } + CENTRO_DISTRIBUICAO, margemPainelPx)
            viewModel.enquadrouFrota = true
            if (modoMotorista) viewModel.seguindo = true
        }
    }

    // Seguindo: a câmera acompanha o veículo a cada passo, sem mexer no zoom do usuário.
    LaunchedEffect(selecionado?.telemetria, viewModel.seguindo) {
        if (viewModel.seguindo && selecionado != null && escolhendoNoMapa == null) {
            mapa.controller.animateTo(selecionado.posicao)
        }
    }

    // Rota nova: enquadra o trajeto inteiro uma vez.
    LaunchedEffect(estadoRota.rota) {
        val pontos = estadoRota.rota?.pontos ?: return@LaunchedEffect
        viewModel.seguindo = false
        enquadrar(mapa, pontos, margemPainelPx)
        sheet.bottomSheetState.partialExpand()
    }

    // Endereço do veículo selecionado (Entrega 7): o ViewModel decide quando consultar a API de novo.
    LaunchedEffect(selecionado?.veiculo?.id, selecionado?.telemetria) {
        enderecoViewModel.acompanhar(
            veiculoId = selecionado?.veiculo?.id,
            posicao = selecionado?.posicao,
            destinoEntrega = selecionado?.takeIf { it.emOperacao }?.viagem?.destino
        )
    }

    // Recados de outras abas: filtrar, focar um veículo ou traçar rota.
    LaunchedEffect(pedido, frotaVisivel != null) {
        val p = pedido ?: return@LaunchedEffect
        val frota = frotaVisivel ?: return@LaunchedEffect
        viewModel.filtro = p.filtro
        p.placa?.let { placa -> frota.find { it.veiculo.placa == placa }?.let(::selecionar) }
        if (p.rotaAte != null) {
            comLocalizacao { rotaViewModel.tracarAte(PontoRota.Fixo(p.rotaAte, p.rotaDescricao ?: "Destino")) }
        }
        PedidosNavegacao.consumirMapa()
    }

    // Endereço ao vivo no modo "escolher no mapa" (só depois que o mapa para de mexer).
    var enderecoCentro by remember { mutableStateOf<Endereco?>(null) }
    var buscandoCentro by remember { mutableStateOf(false) }
    LaunchedEffect(centroMapa, escolhendoNoMapa) {
        if (escolhendoNoMapa == null) return@LaunchedEffect
        buscandoCentro = true
        delay(600)
        enderecoCentro = runCatching { GeocodingService.endereco(centroMapa) }.getOrNull()
        buscandoCentro = false
    }

    BackHandler(enabled = escolhendoNoMapa != null) { escolhendoNoMapa = null }
    BackHandler(enabled = escolhendoNoMapa == null && planejando == null && viewModel.selecionadoId != null && !modoMotorista) {
        viewModel.selecionadoId = null
    }

    val peek = when {
        escolhendoNoMapa != null -> 0.dp
        estadoRota.ativa -> 220.dp
        selecionado != null -> 250.dp
        else -> 150.dp
    }

    Box(Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = sheet,
            sheetPeekHeight = peek,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetShadowElevation = 8.dp,
            containerColor = MaterialTheme.colorScheme.background,
            sheetContent = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 620.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = Espaco.xl)
                ) {
                    when {
                        frotaVisivel == null -> Box(Modifier.fillMaxWidth().padding(Espaco.xl), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        modoMotorista && usuario?.veiculoRemoteId == null -> EstadoVazio(
                            Icons.Filled.LocalShipping,
                            "Nenhum veículo atribuído",
                            "Quando o gestor atribuir um veículo a você, ele aparece aqui com a entrega do dia."
                        )
                        modoMotorista && frotaVisivel.isEmpty() -> EstadoVazio(
                            Icons.Filled.LocalShipping,
                            "Veículo ainda não chegou",
                            "Seu veículo está sendo sincronizado ou aguarda aprovação do gestor."
                        )
                        frotaVisivel.isEmpty() -> EstadoVazio(
                            Icons.Filled.LocalShipping,
                            "Nenhum veículo em operação",
                            "Cadastre e aprove veículos na aba Frota para acompanhá-los aqui."
                        )
                        else -> {
                            if (estadoRota.ativa) {
                                CardRota(
                                    estado = estadoRota,
                                    onRecalcular = {
                                        val destino = estadoRota.destinoEscolhido as? PontoRota.Fixo
                                        val veiculo = frotaVisivel.find { it.veiculo.id == destino?.veiculoId }
                                        rotaViewModel.recalcular(veiculo?.let(::destinoDoVeiculo))
                                    },
                                    onInverter = rotaViewModel::inverter,
                                    onEditar = { planejando = CampoRota.DESTINO },
                                    onLimpar = rotaViewModel::limpar
                                )
                                Spacer(Modifier.height(Espaco.lg))
                            }
                            if (selecionado != null) {
                                PainelVeiculo(
                                    item = selecionado,
                                    enderecos = enderecos.takeIf { it.veiculoId == selecionado.veiculo.id },
                                    seguindo = viewModel.seguindo,
                                    podeFechar = !modoMotorista,
                                    onFechar = { viewModel.selecionadoId = null },
                                    onTracarRota = { comLocalizacao { rotaViewModel.tracarAte(destinoDoVeiculo(selecionado)) } },
                                    onNavegarEntrega = if (modoMotorista && selecionado.emOperacao) {
                                        {
                                            comLocalizacao {
                                                rotaViewModel.tracarAte(PontoRota.Fixo(selecionado.viagem.destino, selecionado.viagem.clienteNome))
                                            }
                                        }
                                    } else null,
                                    onSeguir = { selecionar(selecionado) },
                                    onDetalhes = if (!modoMotorista) { { onAbrirVeiculo(selecionado.veiculo.id) } } else null
                                )
                            } else if (!estadoRota.ativa) {
                                ResumoFrota(frotaVisivel)
                            }
                            if (!modoMotorista) {
                                Spacer(Modifier.height(Espaco.lg))
                                Text(
                                    if (viewModel.filtro == null) "Veículos (${frotaFiltrada.size})"
                                    else "${viewModel.filtro?.label} (${frotaFiltrada.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TrakSyncTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = Espaco.tela)
                                )
                                ListaFrotaMapa(frotaFiltrada, selecionado?.veiculo?.id, ::selecionar)
                            }
                        }
                    }
                }
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { mapa },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Mapa da frota. A lista de veículos está no painel abaixo." },
                    update = { map ->
                        val visiveis = frotaFiltrada.associateBy { it.veiculo.id }
                        // Tira do mapa quem saiu (filtro, exclusão, inativação).
                        marcadores.keys.filter { it !in visiveis }.forEach { id -> map.overlays.remove(marcadores.remove(id)) }
                        linhasViagem.keys.filter { it !in visiveis }.forEach { id -> map.overlays.remove(linhasViagem.remove(id)) }
                        // Trajeto restante: fino e translúcido; o do selecionado, forte.
                        visiveis.forEach { (id, item) ->
                            val linha = linhasViagem.getOrPut(id) {
                                Polyline(map).also { map.overlays.add(1 + circulosCercas.size, it) }
                            }
                            val destaque = id == selecionado?.veiculo?.id
                            linha.setPoints(item.viagem.trajetoRestante)
                            linha.outlinePaint.color = if (destaque) corViagem else ColorUtils.setAlphaComponent(corViagem, 90)
                            linha.outlinePaint.strokeWidth = if (destaque) 12f else 6f
                            linha.isEnabled = item.viagem.trajetoRestante.size > 1 && (destaque || !estadoRota.ativa)
                        }
                        marcadorCd.icon = iconeCd
                        selecionado?.viagem?.let {
                            marcadorEntrega.position = it.destino
                            marcadorEntrega.title = "Entrega: ${it.clienteNome}"
                        }
                        marcadorEntrega.icon = iconeEntrega
                        marcadorEntrega.isEnabled = selecionado?.emOperacao == true
                        marcadorHalo.icon = halo
                        selecionado?.let { marcadorHalo.position = it.posicao }
                        marcadorHalo.isEnabled = selecionado != null && selecionado.veiculo.id in visiveis
                        visiveis.forEach { (id, item) ->
                            val marker = marcadores.getOrPut(id) {
                                Marker(map).also {
                                    it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    it.setOnMarkerClickListener { _, _ -> aoTocarMarcador(id); true }
                                    map.overlays.add(it)
                                }
                            }
                            marker.position = item.posicao
                            marker.icon = icones.getValue(tipoVeiculo(item.veiculo.tipo) to item.status)
                            marker.title = item.veiculo.placa
                        }
                        // Rota: linha por ruas, origem e destino.
                        linhaRota.outlinePaint.color = corRota
                        linhaRota.setPoints(estadoRota.rota?.pontos.orEmpty())
                        estadoRota.origem?.let { marcadorOrigem.position = it }
                        marcadorOrigem.icon = iconeOrigem
                        marcadorOrigem.isEnabled = estadoRota.origem != null
                        estadoRota.destino?.let {
                            marcadorDestino.position = it
                            marcadorDestino.title = estadoRota.destinoEscolhido?.descricao
                        }
                        marcadorDestino.icon = iconeDestino
                        marcadorDestino.isEnabled = estadoRota.destino != null &&
                            (estadoRota.destinoEscolhido as? PontoRota.Fixo)?.veiculoId == null
                        map.invalidate()
                    }
                )

                // ----- Topo: busca e filtros -----
                AnimatedVisibility(escolhendoNoMapa == null, enter = fadeIn(), exit = fadeOut()) {
                    Column(
                        Modifier
                            .statusBarsPadding()
                            .padding(horizontal = Espaco.md, vertical = Espaco.sm)
                    ) {
                        BarraParaOnde(
                            texto = estadoRota.destinoEscolhido?.let { "Até ${it.descricao}" } ?: "Para onde?",
                            ativa = estadoRota.ativa,
                            onClick = { planejando = CampoRota.DESTINO },
                            onLimpar = rotaViewModel::limpar
                        )
                        if (!modoMotorista && !frotaVisivel.isNullOrEmpty()) {
                            Spacer(Modifier.height(Espaco.sm))
                            FiltrosStatus(
                                frota = frotaVisivel,
                                filtro = viewModel.filtro,
                                mostrarCercas = viewModel.mostrarCercas,
                                temCercas = cercas.any { it.ativa },
                                onFiltro = { viewModel.filtro = it },
                                onAlternarCercas = { viewModel.mostrarCercas = !viewModel.mostrarCercas }
                            )
                        }
                        // Saiu do "seguir" mexendo no mapa: um toque volta a acompanhar.
                        AnimatedVisibility(selecionado != null && !viewModel.seguindo) {
                            AssistChip(
                                onClick = { selecionado?.let(::selecionar) },
                                label = { Text("Seguir ${selecionado?.veiculo?.placa.orEmpty()}") },
                                leadingIcon = { Icon(Icons.Filled.GpsFixed, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.padding(top = Espaco.xs)
                            )
                        }
                    }
                }

                // ----- Botões flutuantes -----
                if (escolhendoNoMapa == null) {
                    Column(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = Espaco.md, bottom = peek + Espaco.md),
                        verticalArrangement = Arrangement.spacedBy(Espaco.sm)
                    ) {
                        BotaoMapa(Icons.Filled.Add, "Aproximar") { mapa.controller.zoomIn() }
                        BotaoMapa(Icons.Filled.Remove, "Afastar") { mapa.controller.zoomOut() }
                        BotaoMapa(Icons.Filled.MyLocation, "Minha localização") {
                            comLocalizacao {
                                escopo.launch {
                                    viewModel.seguindo = false
                                    localizacaoAtual(context)?.let { mapa.controller.animateTo(it, maxOf(mapa.zoomLevelDouble, 16.0), 600L) }
                                }
                            }
                        }
                        BotaoMapa(Icons.Filled.CenterFocusStrong, if (modoMotorista) "Centralizar no meu veículo" else "Mostrar a frota inteira") {
                            viewModel.seguindo = false
                            if (modoMotorista) selecionado?.let(::selecionar)
                            else enquadrar(mapa, frotaFiltrada.map { it.posicao } + CENTRO_DISTRIBUICAO, margemPainelPx)
                        }
                    }
                }

                // ----- Escolher um ponto no mapa -----
                if (escolhendoNoMapa != null) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-22).dp)
                            .size(48.dp)
                    )
                    Surface(
                        Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(Espaco.md),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.inverseSurface
                    ) {
                        Text(
                            "Arraste o mapa até o ponto desejado",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = Espaco.lg, vertical = Espaco.sm)
                        )
                    }
                    Surface(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(Espaco.md)
                            .navigationBarsPadding(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp
                    ) {
                        Column(Modifier.padding(Espaco.lg)) {
                            Text(
                                if (escolhendoNoMapa == CampoRota.ORIGEM) "Origem" else "Destino",
                                style = MaterialTheme.typography.labelLarge,
                                color = TrakSyncTheme.colors.textSecondary
                            )
                            Text(
                                when {
                                    buscandoCentro -> "Buscando endereço..."
                                    enderecoCentro != null -> enderecoCentro!!.resumo
                                    else -> "%.5f, %.5f".format(centroMapa.latitude, centroMapa.longitude)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(Espaco.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                                BotaoSecundario("Cancelar", null, { escolhendoNoMapa = null }, Modifier.weight(1f))
                                BotaoPrincipal(
                                    "Confirmar ponto",
                                    onClick = {
                                        val campo = escolhendoNoMapa ?: return@BotaoPrincipal
                                        definirPonto(campo, PontoRota.Fixo(centroMapa, enderecoCentro?.linha1 ?: "Ponto escolhido no mapa"))
                                        escolhendoNoMapa = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ----- Planejador por cima de tudo -----
        planejando?.let { campoInicial ->
            BackHandler { planejando = null }
            PlanejadorRota(
                origem = estadoRota.origemEscolhida,
                destino = estadoRota.destinoEscolhido,
                campoInicial = campoInicial,
                frota = if (modoMotorista) emptyList() else frotaVisivel.orEmpty(),
                entregaAtual = selecionado?.takeIf { modoMotorista && it.emOperacao }
                    ?.let { PontoRota.Fixo(it.viagem.destino, it.viagem.clienteNome) },
                onEscolher = { campo, ponto ->
                    definirPonto(campo, ponto)
                    if (campo == CampoRota.DESTINO || estadoRota.destinoEscolhido != null) planejando = null
                },
                onEscolherNoMapa = { campo ->
                    planejando = null
                    viewModel.seguindo = false
                    escolhendoNoMapa = campo
                    centroMapa = GeoPoint(mapa.mapCenter.latitude, mapa.mapCenter.longitude)
                },
                onInverter = rotaViewModel::inverter,
                onFechar = { planejando = null }
            )
        }
    }
}

/** Barra de busca no topo do mapa: abre o planejador de rotas. */
@Composable
private fun BarraParaOnde(texto: String, ativa: Boolean, onClick: () -> Unit, onLimpar: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
    ) {
        Row(Modifier.padding(start = Espaco.lg, end = Espaco.xs), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ativa) Icons.Filled.Directions else Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(Espaco.md))
            Text(
                texto,
                style = MaterialTheme.typography.bodyLarge,
                color = if (ativa) MaterialTheme.colorScheme.onSurface else TrakSyncTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp)
            )
            if (ativa) {
                IconButton(onClick = onLimpar) { Icon(Icons.Filled.Close, contentDescription = "Fechar rota") }
            }
        }
    }
}

@Composable
private fun BotaoMapa(icone: androidx.compose.ui.graphics.vector.ImageVector, descricao: String, onClick: () -> Unit) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(icone, contentDescription = descricao)
    }
}

/** Resumo no painel quando nada está selecionado: contagem por status. */
@Composable
private fun ResumoFrota(frota: List<VeiculoNoMapa>) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Espaco.tela),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatusVeiculo.entries.forEach { status ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                Text(frota.count { it.status == status }.toString(), style = MaterialTheme.typography.headlineSmall, color = corDoStatus(status))
                Text(status.label, style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
            }
        }
    }
    Text(
        "Toque num veículo para ver telemetria, endereço e traçar rota.",
        style = MaterialTheme.typography.bodySmall,
        color = TrakSyncTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = Espaco.tela, vertical = Espaco.md)
    )
}
