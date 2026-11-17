package br.com.rastreadorfrota.ui.screens

import android.graphics.DashPathEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoEntrega
import br.com.rastreadorfrota.rota.formatarDistancia
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import br.com.rastreadorfrota.simulacao.comprimentoMetros
import br.com.rastreadorfrota.simulacao.viagemDaEntrega
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.DialogoConfirmacao
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.LinhaDado
import br.com.rastreadorfrota.ui.components.SecaoTitulo
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.enquadrar
import br.com.rastreadorfrota.ui.components.iconeMarcador
import br.com.rastreadorfrota.ui.components.rememberMapaOsm
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.EntregaViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Histórico de uma entrega: rota planejada (cinza tracejada), por onde o veículo passou (azul)
 * e os trechos fora da rota (vermelho), com horários e distâncias. Enquanto a entrega está em
 * andamento a tela acompanha ao vivo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoEntregaScreen(
    entregaId: Long,
    usuario: Usuario?,
    onBack: () -> Unit,
    onVerNoMapa: (String) -> Unit,
    viewModel: EntregaViewModel = viewModel()
) {
    val entrega by remember(entregaId) { viewModel.observar(entregaId) }.collectAsStateWithLifecycle(initialValue = null)
    // Atualiza a cada passo da simulação enquanto a tela está aberta.
    val passo by SimuladorFrota.passos.collectAsStateWithLifecycle(initialValue = System.currentTimeMillis() / SimuladorFrota.INTERVALO_MS)
    var confirmarCancelamento by remember { mutableStateOf(false) }
    val avisoFila by viewModel.avisoFila.collectAsStateWithLifecycle()
    val ehGestor = usuario?.perfil == Perfil.CONTROLADOR

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(entrega?.let { "Entrega · ${it.placa}" } ?: "Entrega") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        val e = entrega
        if (e == null) {
            EstadoVazio(Icons.Filled.Map, "Entrega não encontrada", "Ela pode ter sido removida em outro aparelho.", Modifier.padding(padding))
            return@Scaffold
        }
        val agora = passo * SimuladorFrota.INTERVALO_MS
        val situacao = e.situacao(agora)
        val viagem = remember(e.remoteId, e.id, e.rotaIdaExecutada) { viagemDaEntrega(e) }
        // Até onde o veículo chegou: agora (em andamento), no cancelamento, ou a viagem toda.
        val ate = when (situacao) {
            SituacaoEntrega.AGENDADA -> -1
            SituacaoEntrega.EM_ANDAMENTO -> SimuladorFrota.indiceNaViagem(e, passo).toInt()
            SituacaoEntrega.CANCELADA -> SimuladorFrota.indiceNaViagem(e, e.canceladaEm!! / SimuladorFrota.INTERVALO_MS).toInt()
            // Encadeada, a entrega termina antes do fim natural (sem a volta ao CD).
            SituacaoEntrega.CONCLUIDA -> ((e.fimPrevistoEm - e.inicioEm) / SimuladorFrota.INTERVALO_MS - 1).toInt()
        }.coerceAtMost(viagem.passos.lastIndex)
        // Seguiu para a próxima entrega da fila em vez de voltar ao CD.
        val encadeada = e.fimPrevistoEm < e.inicioEm + viagem.duracaoMs
        val voltaPlanejada = if (encadeada) emptyList() else viagem.planejado(EtapaViagem.VOLTA)
        val percorrido = viagem.percorrido(ate)
        val trechosFora = viagem.trechosForaDaRota(ate)
        val chegadaCliente = viagem.inicioDaEtapa(EtapaViagem.ENTREGA)?.let { e.inicioEm + it * SimuladorFrota.INTERVALO_MS }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            MapaHistorico(e, viagem.planejado(EtapaViagem.IDA), voltaPlanejada, percorrido, trechosFora, situacao == SituacaoEntrega.EM_ANDAMENTO)

            Column(Modifier.padding(Espaco.tela), verticalArrangement = Arrangement.spacedBy(Espaco.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(Espaco.sm))
                    Column(Modifier.weight(1f)) {
                        Text(e.clienteNome, style = MaterialTheme.typography.titleLarge)
                        Text(e.endereco, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                    }
                    StatusPill(situacao.label, corDaSituacaoEntrega(situacao), iconeDaSituacaoEntrega(situacao))
                }

                if (trechosFora.isNotEmpty()) {
                    CardTrak(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(Espaco.md))
                            Column {
                                Text(
                                    "${trechosFora.size} desvio(s) da rota planejada",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    "${formatarDistancia(trechosFora.sumOf { comprimentoMetros(it) })} percorridos fora da rota (trecho em vermelho no mapa).",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                SecaoTitulo("Linha do tempo")
                CardTrak(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = Espaco.lg, vertical = Espaco.sm)) {
                        LinhaDado("Atribuída", data(e.criadaEm))
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado(
                            if (situacao == SituacaoEntrega.AGENDADA) "Saída prevista" else "Saída",
                            "${data(e.inicioEm)} · ${e.origemNome}"
                        )
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado(
                            if (chegadaCliente != null && chegadaCliente <= agora) "Chegada ao cliente" else "Chegada prevista",
                            chegadaCliente?.let(::data) ?: "—"
                        )
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        when {
                            e.canceladaEm != null -> LinhaDado("Cancelada", data(e.canceladaEm))
                            encadeada -> LinhaDado(
                                if (situacao == SituacaoEntrega.CONCLUIDA) "Seguiu para a próxima" else "Segue para a próxima",
                                data(e.fimPrevistoEm)
                            )
                            else -> LinhaDado(if (situacao == SituacaoEntrega.CONCLUIDA) "Volta ao CD" else "Volta prevista", data(e.fimPrevistoEm))
                        }
                    }
                }

                SecaoTitulo("Detalhes")
                CardTrak(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = Espaco.lg, vertical = Espaco.sm)) {
                        LinhaDado("Veículo", e.placa)
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Motorista", e.motoristaNome ?: "—")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Rota planejada", formatarDistancia(comprimentoMetros(viagem.planejado(EtapaViagem.IDA)) + comprimentoMetros(voltaPlanejada)))
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Percorrido até agora", formatarDistancia(comprimentoMetros(percorrido)))
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Sincronização", if (e.sincronizado) "Na nuvem" else "Pendente")
                    }
                }

                avisoFila?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                Row(horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                    if (situacao == SituacaoEntrega.EM_ANDAMENTO) {
                        BotaoSecundario("Ver ao vivo", Icons.Filled.Map, { onVerNoMapa(e.placa) }, Modifier.weight(1f))
                    }
                    if (ehGestor && e.ativa(agora)) {
                        BotaoPrincipal("Cancelar entrega", { confirmarCancelamento = true }, Modifier.weight(1f), icone = Icons.Filled.Cancel)
                    }
                }
            }
        }

        if (confirmarCancelamento) {
            DialogoConfirmacao(
                titulo = "Cancelar a entrega para ${e.clienteNome}?",
                mensagem = "As próximas entregas da fila de ${e.placa} continuam: o veículo segue para a próxima " +
                    "(a rota é recalculada se precisar). Sem outra na fila, ele volta ao pátio do CD. O histórico até aqui é mantido.",
                confirmar = "Cancelar entrega",
                destrutivo = true,
                icone = Icons.Filled.Cancel,
                onConfirmar = {
                    confirmarCancelamento = false
                    viewModel.cancelar(e)
                },
                onCancelar = { confirmarCancelamento = false }
            )
        }
    }
}

@Composable
private fun MapaHistorico(
    entrega: EntregaEntity,
    idaPlanejada: List<GeoPoint>,
    voltaPlanejada: List<GeoPoint>,
    percorrido: List<GeoPoint>,
    trechosFora: List<List<GeoPoint>>,
    aoVivo: Boolean
) {
    val mapa = rememberMapaOsm()
    val corPlanejada = TrakSyncTheme.colors.textSecondary.toArgb()
    val corPercorrido = MaterialTheme.colorScheme.primary.toArgb()
    val corDesvio = MaterialTheme.colorScheme.error.toArgb()
    val destino = GeoPoint(entrega.latitude, entrega.longitude)
    val tracejado = remember { DashPathEffect(floatArrayOf(28f, 18f), 0f) }
    val linhaIda = remember { Polyline(mapa).apply { outlinePaint.strokeWidth = 9f; outlinePaint.pathEffect = tracejado } }
    val linhaVolta = remember { Polyline(mapa).apply { outlinePaint.strokeWidth = 9f; outlinePaint.pathEffect = tracejado } }
    val linhaPercorrido = remember { Polyline(mapa).apply { outlinePaint.strokeWidth = 11f } }
    val linhasDesvio = remember { mutableListOf<Polyline>() }
    val marcadorVeiculo = remember {
        Marker(mapa).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = entrega.placa
            icon = iconeMarcador(mapa, corPercorrido)
        }
    }
    remember {
        mapa.overlays.addAll(listOf(linhaIda, linhaVolta, linhaPercorrido))
        mapa.overlays.add(Marker(mapa).apply {
            position = CENTRO_DISTRIBUICAO; title = NOME_CD
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = iconeMarcador(mapa, corPlanejada)
        })
        mapa.overlays.add(Marker(mapa).apply {
            position = destino; title = entrega.clienteNome
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = iconeMarcador(mapa, corDesvio)
        })
        mapa.overlays.add(marcadorVeiculo)
        true
    }
    LaunchedEffect(entrega.id) { enquadrar(mapa, idaPlanejada + voltaPlanejada) }

    AndroidView(
        factory = { mapa },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clipToBounds() // o MapView desenha fora dos limites sem isso
            .semantics { contentDescription = "Mapa da entrega: rota planejada tracejada, percorrido em azul e desvios em vermelho" },
        update = { m ->
            linhaIda.outlinePaint.color = corPlanejada
            linhaVolta.outlinePaint.color = corPlanejada
            linhaIda.setPoints(idaPlanejada)
            linhaVolta.setPoints(voltaPlanejada)
            linhaPercorrido.outlinePaint.color = corPercorrido
            linhaPercorrido.setPoints(percorrido)
            while (linhasDesvio.size < trechosFora.size) {
                linhasDesvio += Polyline(m).apply { outlinePaint.strokeWidth = 12f }.also { m.overlays.add(m.overlays.indexOf(linhaPercorrido) + 1, it) }
            }
            linhasDesvio.forEachIndexed { i, l ->
                l.outlinePaint.color = corDesvio
                l.setPoints(trechosFora.getOrNull(i).orEmpty())
            }
            percorrido.lastOrNull()?.let { marcadorVeiculo.position = it }
            marcadorVeiculo.isEnabled = aoVivo && percorrido.isNotEmpty()
            m.invalidate()
        }
    )
}

@Composable
fun corDaSituacaoEntrega(s: SituacaoEntrega): Color = when (s) {
    SituacaoEntrega.AGENDADA -> TrakSyncTheme.colors.warning
    SituacaoEntrega.EM_ANDAMENTO -> MaterialTheme.colorScheme.primary
    SituacaoEntrega.CONCLUIDA -> TrakSyncTheme.colors.success
    SituacaoEntrega.CANCELADA -> TrakSyncTheme.colors.textSecondary
}

fun iconeDaSituacaoEntrega(s: SituacaoEntrega) = when (s) {
    SituacaoEntrega.AGENDADA -> Icons.Filled.Schedule
    SituacaoEntrega.EM_ANDAMENTO -> Icons.Filled.Map
    SituacaoEntrega.CONCLUIDA -> Icons.Filled.CheckCircle
    SituacaoEntrega.CANCELADA -> Icons.Filled.Cancel
}

private fun data(instante: Long) = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR")).format(Date(instante))
