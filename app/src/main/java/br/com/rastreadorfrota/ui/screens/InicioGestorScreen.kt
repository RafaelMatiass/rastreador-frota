package br.com.rastreadorfrota.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.PreferenciasApp
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import br.com.rastreadorfrota.data.sync.SyncStatus
import br.com.rastreadorfrota.geofence.NotificacaoAlertas
import br.com.rastreadorfrota.navigation.PedidoMapa
import br.com.rastreadorfrota.navigation.PedidosNavegacao
import br.com.rastreadorfrota.navigation.Rotas
import br.com.rastreadorfrota.rota.formatarDuracao
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import br.com.rastreadorfrota.ui.components.AvisoPermissao
import br.com.rastreadorfrota.ui.components.BarraEmpilhada
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.ChipSincronizacao
import br.com.rastreadorfrota.ui.components.Esqueleto
import br.com.rastreadorfrota.ui.components.FatiaGrafico
import br.com.rastreadorfrota.ui.components.GraficoDonut
import br.com.rastreadorfrota.ui.components.KpiCard
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.SecaoTitulo
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.components.corDoStatus
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.DashboardViewModel
import br.com.rastreadorfrota.ui.viewmodel.EstadoDashboard
import br.com.rastreadorfrota.ui.viewmodel.ItemAtencao
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel
import br.com.rastreadorfrota.ui.viewmodel.TipoAtencao
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Saudação pelo horário do aparelho. */
fun saudacao(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Bom dia"
    in 12..17 -> "Boa tarde"
    else -> "Boa noite"
}

/**
 * Início do gestor: a operação de agora em uma tela. Tudo é tocável e leva ao lugar certo
 * (mapa filtrado, veículo focado, aprovação, alertas).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioGestorScreen(
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    onAbrirAba: (String) -> Unit,
    onAbrirVeiculo: (Long) -> Unit,
    viewModel: DashboardViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    veiculoViewModel: VeiculoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val isOnline by syncViewModel.isOnline.collectAsStateWithLifecycle()
    val status by syncViewModel.status.collectAsStateWithLifecycle()
    val pendentes by syncViewModel.pendentes.collectAsStateWithLifecycle()

    fun abrirMapa(pedido: PedidoMapa) {
        PedidosNavegacao.pedirMapa(pedido)
        onAbrirAba(Rotas.MAPA)
    }

    PullToRefreshBox(
        isRefreshing = status is SyncStatus.Syncing,
        onRefresh = syncViewModel::sincronizarAgora,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = Espaco.xl),
            verticalArrangement = Arrangement.spacedBy(Espaco.lg)
        ) {
            item {
                TopoAba(
                    titulo = "${saudacao()}, ${usuario?.nome?.substringBefore(" ").orEmpty()}",
                    subtitulo = usuario?.empresa?.ifBlank { null } ?: "Painel da frota",
                    usuario = usuario,
                    onAbrirPerfil = onAbrirPerfil
                )
                ChipSincronizacao(
                    isOnline = isOnline,
                    status = status,
                    pendentes = pendentes,
                    onSincronizar = syncViewModel::sincronizarAgora,
                    modifier = Modifier.padding(horizontal = Espaco.tela)
                )
            }
            item { PedidoNotificacoes() }

            val e = estado
            if (e == null) {
                item { EsqueletoDashboard() }
                return@LazyColumn
            }

            item { CardFrotaAgora(e, onFiltrar = { abrirMapa(PedidoMapa(filtro = it)) }) }
            item {
                Column(Modifier.padding(horizontal = Espaco.tela), verticalArrangement = Arrangement.spacedBy(Espaco.md)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.md)) {
                        KpiCard(
                            Icons.Filled.NearMe, e.contar(EtapaViagem.IDA), "A caminho de clientes", TrakSyncTheme.colors.success,
                            Modifier.weight(1f), onClick = { abrirMapa(PedidoMapa(filtro = StatusVeiculo.EM_TRANSITO)) }
                        )
                        KpiCard(
                            Icons.Filled.Storefront, e.contar(EtapaViagem.ENTREGA), "Entregando agora", MaterialTheme.colorScheme.error,
                            Modifier.weight(1f), onClick = { abrirMapa(PedidoMapa(filtro = StatusVeiculo.PARADO)) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Espaco.md)) {
                        KpiCard(
                            Icons.Filled.Speed, e.velocidadeMedia, "Velocidade média em trânsito", MaterialTheme.colorScheme.primary,
                            Modifier.weight(1f), sufixo = "km/h"
                        )
                        KpiCard(
                            Icons.Filled.NotificationsActive, e.alertasHoje, "Alertas de cerca hoje", MaterialTheme.colorScheme.tertiary,
                            Modifier.weight(1f), onClick = { onAbrirAba(Rotas.ALERTAS) }
                        )
                    }
                }
            }

            item {
                SecaoTitulo("Precisa de atenção", Modifier.padding(horizontal = Espaco.tela))
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    if (e.atencao.isEmpty()) {
                        Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TrakSyncTheme.colors.success)
                            Spacer(Modifier.width(Espaco.md))
                            Text("Tudo certo por aqui. Nenhuma pendência na frota.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        Column {
                            e.atencao.forEachIndexed { i, item ->
                                LinhaAtencao(
                                    item,
                                    onAprovar = item.veiculo?.let { v -> { veiculoViewModel.aprovar(v) } },
                                    onClick = {
                                        when {
                                            item.veiculo != null -> onAbrirVeiculo(item.veiculo.id)
                                            item.placa != null -> abrirMapa(PedidoMapa(placa = item.placa))
                                            item.tipo == TipoAtencao.SEM_MOTORISTA || item.tipo == TipoAtencao.MOTORISTA_SEM_VEICULO ||
                                                item.tipo == TipoAtencao.CNH_VENCENDO -> onAbrirAba(Rotas.EQUIPE)
                                        }
                                    }
                                )
                                if (i < e.atencao.lastIndex) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                            }
                        }
                    }
                }
            }

            if (e.proximasChegadas.isNotEmpty()) {
                item {
                    SecaoTitulo("Próximas chegadas", Modifier.padding(horizontal = Espaco.tela), "Ver no mapa") { onAbrirAba(Rotas.MAPA) }
                    CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                        Column {
                            e.proximasChegadas.take(4).forEachIndexed { i, item ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable(onClickLabel = "Mostrar no mapa") { abrirMapa(PedidoMapa(placa = item.veiculo.placa)) }
                                        .padding(horizontal = Espaco.lg, vertical = Espaco.md)
                                        .semantics(mergeDescendants = true) {},
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlacaBadge(item.veiculo.placa, compacta = true)
                                    Spacer(Modifier.width(Espaco.md))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.viagem.clienteNome, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            item.motorista?.nome ?: item.veiculo.modelo,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TrakSyncTheme.colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(formatarDuracao(item.viagem.segundosRestantes.toDouble()), style = MaterialTheme.typography.titleSmall, color = TrakSyncTheme.colors.success)
                                        Text("para chegar", style = MaterialTheme.typography.labelSmall, color = TrakSyncTheme.colors.textSecondary)
                                    }
                                }
                                if (i < minOf(3, e.proximasChegadas.lastIndex)) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                            }
                        }
                    }
                }
            }

            item {
                SecaoTitulo("Etapas das viagens", Modifier.padding(horizontal = Espaco.tela))
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    BarraEmpilhada(
                        listOf(
                            FatiaGrafico("Ida", e.contar(EtapaViagem.IDA), TrakSyncTheme.colors.success),
                            FatiaGrafico("Entrega", e.contar(EtapaViagem.ENTREGA), MaterialTheme.colorScheme.error),
                            FatiaGrafico("Volta", e.contar(EtapaViagem.VOLTA), MaterialTheme.colorScheme.primary),
                            FatiaGrafico("No CD", e.contar(EtapaViagem.DESCARGA) + e.contar(EtapaViagem.FORA_DE_OPERACAO), TrakSyncTheme.colors.textSecondary)
                        ),
                        Modifier.padding(Espaco.lg)
                    )
                }
            }

            item {
                SecaoTitulo("Atividade recente", Modifier.padding(horizontal = Espaco.tela), "Ver todos") { onAbrirAba(Rotas.ALERTAS) }
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    if (e.alertasRecentes.isEmpty()) {
                        Text(
                            "Sem alertas ainda. Crie cercas na aba Alertas para saber quando um veículo chega ou sai de um lugar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TrakSyncTheme.colors.textSecondary,
                            modifier = Modifier.padding(Espaco.lg)
                        )
                    } else {
                        Column {
                            e.alertasRecentes.forEachIndexed { i, alerta ->
                                LinhaAlertaCompacta(alerta) { abrirMapa(PedidoMapa(placa = alerta.placa)) }
                                if (i < e.alertasRecentes.lastIndex) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Pede a permissão de notificação (Android 13+) com explicação, uma vez. */
@Composable
private fun PedidoNotificacoes() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val prefs = remember { PreferenciasApp(context) }
    var mostrar by rememberSaveable { mutableStateOf(!NotificacaoAlertas.temPermissao(context) && !prefs.pediuNotificacoes) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs.pediuNotificacoes = true
        mostrar = false
    }
    if (!mostrar) return
    AvisoPermissao(
        icone = Icons.Filled.NotificationsActive,
        titulo = "Receba os alertas de cerca",
        mensagem = "Avisamos quando um veículo chega ou sai do CD e dos clientes, mesmo com o app em segundo plano.",
        botao = "Ativar notificações",
        onPermitir = { pedir.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onDispensar = { prefs.pediuNotificacoes = true; mostrar = false },
        modifier = Modifier.padding(horizontal = Espaco.tela)
    )
}

@Composable
private fun CardFrotaAgora(e: EstadoDashboard, onFiltrar: (StatusVeiculo?) -> Unit) {
    CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
        Column(Modifier.padding(Espaco.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Frota agora", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${e.totalVeiculos} veículos · ${e.motoristasAtivos} motoristas",
                    style = MaterialTheme.typography.labelMedium,
                    color = TrakSyncTheme.colors.textSecondary
                )
            }
            Spacer(Modifier.height(Espaco.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GraficoDonut(
                    fatias = StatusVeiculo.entries.map { FatiaGrafico(it.label, e.contar(it), corDoStatus(it)) },
                    centroValor = e.frota.size.toString(),
                    centroRotulo = "em operação",
                    tamanho = 132.dp
                )
                Spacer(Modifier.width(Espaco.lg))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Espaco.xs)) {
                    StatusVeiculo.entries.forEach { status ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClickLabel = "Ver no mapa") { onFiltrar(status) }
                                .padding(horizontal = Espaco.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).background(corDoStatus(status), CircleShape))
                            Spacer(Modifier.width(Espaco.sm))
                            Text(status.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(e.contar(status).toString(), style = MaterialTheme.typography.titleMedium)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TrakSyncTheme.colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaAtencao(item: ItemAtencao, onAprovar: (() -> Unit)?, onClick: () -> Unit) {
    val (icone, cor) = iconeAtencao(item.tipo)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Espaco.lg, vertical = Espaco.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(cor.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(Espaco.md))
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(item.titulo, style = MaterialTheme.typography.titleSmall)
            Text(item.detalhe, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary, maxLines = 2)
        }
        if (onAprovar != null) {
            Spacer(Modifier.width(Espaco.sm))
            Button(onClick = onAprovar, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Aprovar") }
        }
    }
}

@Composable
private fun iconeAtencao(tipo: TipoAtencao): Pair<ImageVector, Color> = when (tipo) {
    TipoAtencao.PENDENTE -> Icons.Filled.HourglassTop to TrakSyncTheme.colors.warning
    TipoAtencao.CNH_VENCENDO -> Icons.Filled.Badge to TrakSyncTheme.colors.warning
    TipoAtencao.PORTA_ABERTA -> Icons.Filled.SensorDoor to MaterialTheme.colorScheme.error
    TipoAtencao.VELOCIDADE -> Icons.Filled.Speed to MaterialTheme.colorScheme.error
    TipoAtencao.SEM_MOTORISTA -> StatusVeiculo.SEM_MOTORISTA.icone to TrakSyncTheme.colors.textSecondary
    TipoAtencao.MOTORISTA_SEM_VEICULO -> Icons.Filled.PersonOff to TrakSyncTheme.colors.textSecondary
}

/** Linha de alerta usada no dashboard e no Início do motorista. */
@Composable
fun LinhaAlertaCompacta(alerta: AlertaEntity, onClick: (() -> Unit)? = null) {
    val entrada = alerta.tipoEnum == TipoAlerta.ENTRADA
    val cor = if (entrada) TrakSyncTheme.colors.success else TrakSyncTheme.colors.warning
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = Espaco.lg, vertical = Espaco.md)
            .semantics(mergeDescendants = true) { contentDescription = "${alerta.descricao} às ${hora(alerta.instante)}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(cor.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                if (entrada) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(Espaco.md))
        Column(Modifier.weight(1f)) {
            Text(alerta.descricao, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(alerta.motoristaNome?.substringBefore(" "), alerta.origemEnum.label).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = TrakSyncTheme.colors.textSecondary
            )
        }
        Text(hora(alerta.instante), style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
    }
}

fun hora(instante: Long): String = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(instante))

@Composable
private fun EsqueletoDashboard() {
    Column(Modifier.padding(horizontal = Espaco.tela), verticalArrangement = Arrangement.spacedBy(Espaco.md)) {
        Esqueleto(Modifier.fillMaxWidth().height(180.dp), cantos = 20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(Espaco.md)) {
            Esqueleto(Modifier.weight(1f).height(120.dp), cantos = 20.dp)
            Esqueleto(Modifier.weight(1f).height(120.dp), cantos = 20.dp)
        }
        Esqueleto(Modifier.fillMaxWidth().height(140.dp), cantos = 20.dp)
    }
}
