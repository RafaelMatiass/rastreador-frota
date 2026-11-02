package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.AlvoCerca
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.rota.formatarDistancia
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.CercaViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Aba Alertas (gestor): histórico de entradas/saídas e as cercas que geram esses alertas. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AlertasScreen(
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    onNovaCerca: () -> Unit,
    onEditarCerca: (Long) -> Unit,
    onVerNoMapa: (String) -> Unit,
    viewModel: CercaViewModel = viewModel()
) {
    val alertas by viewModel.alertas.collectAsStateWithLifecycle()
    val cercas by viewModel.cercas.collectAsStateWithLifecycle()
    var aba by rememberSaveable { mutableIntStateOf(0) }

    // Saiu da aba depois de ver os eventos: eles deixam de contar como "não lidos".
    DisposableEffect(Unit) { onDispose { viewModel.marcarTodosLidos() } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopoAba(
                "Alertas",
                usuario,
                onAbrirPerfil,
                subtitulo = "Geofencing da frota",
                acoes = {
                    if (aba == 0 && alertas.orEmpty().any { !it.lido }) {
                        IconButton(onClick = viewModel::marcarTodosLidos) { Icon(Icons.Filled.DoneAll, contentDescription = "Marcar todos como lidos") }
                    }
                }
            )
            SecondaryTabRow(selectedTabIndex = aba, containerColor = MaterialTheme.colorScheme.background) {
                Tab(selected = aba == 0, onClick = { aba = 0 }, text = { Text("Eventos") })
                Tab(selected = aba == 1, onClick = { aba = 1 }, text = { Text("Cercas (${cercas.size})") })
            }
            if (aba == 0) ListaEventos(alertas.orEmpty(), temCercas = cercas.isNotEmpty(), onVerNoMapa, onCriarCerca = { aba = 1 })
            else ListaCercas(cercas, viewModel::alternarAtiva, onEditarCerca, viewModel::criarCercaDoCd)
        }
        if (aba == 1) {
            ExtendedFloatingActionButton(
                onClick = onNovaCerca,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nova cerca") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(Espaco.lg)
            )
        }
    }
}

@Composable
private fun ListaEventos(alertas: List<AlertaEntity>, temCercas: Boolean, onVerNoMapa: (String) -> Unit, onCriarCerca: () -> Unit) {
    if (alertas.isEmpty()) {
        EstadoVazio(
            Icons.Filled.NotificationsNone,
            "Nenhum alerta ainda",
            if (temCercas) "Assim que um veículo entrar ou sair de uma cerca, o evento aparece aqui e você recebe uma notificação."
            else "Crie uma cerca (no CD ou num cliente) para começar a receber alertas de entrada e saída.",
            acao = if (temCercas) null else "Criar cerca",
            onAcao = onCriarCerca
        )
        return
    }
    val porDia = alertas.groupBy { rotuloDia(it.instante) }
    LazyColumn(contentPadding = PaddingValues(bottom = Espaco.xl)) {
        porDia.forEach { (dia, doDia) ->
            item(key = "dia-$dia") {
                Text(
                    dia,
                    style = MaterialTheme.typography.labelLarge,
                    color = TrakSyncTheme.colors.textSecondary,
                    modifier = Modifier.padding(start = Espaco.tela, top = Espaco.lg, bottom = Espaco.xs)
                )
            }
            items(doDia, key = { it.id }) { alerta ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ponto azul nos não lidos.
                    Box(
                        Modifier
                            .padding(start = Espaco.sm)
                            .size(8.dp)
                            .background(if (alerta.lido) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Box(Modifier.weight(1f)) { LinhaAlertaCompacta(alerta) { onVerNoMapa(alerta.placa) } }
                }
                HorizontalDivider(Modifier.padding(start = 72.dp), color = TrakSyncTheme.colors.surface2)
            }
        }
    }
}

@Composable
private fun ListaCercas(
    cercas: List<CercaEntity>,
    onAlternar: (CercaEntity) -> Unit,
    onEditar: (Long) -> Unit,
    onCriarCd: () -> Unit
) {
    if (cercas.isEmpty()) {
        EstadoVazio(
            Icons.Filled.Radar,
            "Nenhuma cerca criada",
            "Cercas são áreas no mapa. Comece pelo centro de distribuição para saber quando cada veículo sai e volta.",
            acao = "Criar cerca do CD (200 m)",
            onAcao = onCriarCd
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = Espaco.tela, end = Espaco.tela, top = Espaco.md, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(Espaco.md)
    ) {
        items(cercas, key = { it.id }) { cerca ->
            CardTrak(Modifier.fillMaxWidth(), onClick = { onEditar(cerca.id) }) {
                Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = if (cerca.ativa) 0.16f else 0.06f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Radar, contentDescription = null, tint = if (cerca.ativa) MaterialTheme.colorScheme.tertiary else TrakSyncTheme.colors.textSecondary)
                    }
                    Spacer(Modifier.width(Espaco.md))
                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                        Text(cerca.nome, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Raio ${formatarDistancia(cerca.raioM)} · ${descricaoAlvo(cerca)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TrakSyncTheme.colors.textSecondary
                        )
                        Text(
                            listOfNotNull("entrada".takeIf { cerca.notificarEntrada }, "saída".takeIf { cerca.notificarSaida })
                                .joinToString(" e ").ifBlank { "sem avisos" }.replaceFirstChar { it.uppercase() } +
                                if (!cerca.sincronizado) " · aguardando sincronizar" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = TrakSyncTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = cerca.ativa,
                        onCheckedChange = { onAlternar(cerca) },
                        modifier = Modifier.semantics {
                            contentDescription = "Monitorar ${cerca.nome}"
                            stateDescription = if (cerca.ativa) "Ativa" else "Pausada"
                        }
                    )
                }
            }
        }
    }
}

fun descricaoAlvo(cerca: CercaEntity): String = when (cerca.alvoEnum) {
    AlvoCerca.TODOS -> "toda a frota"
    AlvoCerca.TIPO -> "${tipoVeiculo(cerca.alvoValor).label.lowercase()}s"
    AlvoCerca.VEICULOS -> cerca.placasAlvo.joinToString().ifBlank { "nenhum veículo" }
}

private fun rotuloDia(instante: Long): String {
    val dia = Calendar.getInstance().apply { timeInMillis = instante }
    val hoje = Calendar.getInstance()
    fun mesmoDia(a: Calendar, b: Calendar) = a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val ontem = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        mesmoDia(dia, hoje) -> "Hoje"
        mesmoDia(dia, ontem) -> "Ontem"
        else -> SimpleDateFormat("EEEE, dd/MM", Locale("pt", "BR")).format(Date(instante)).replaceFirstChar { it.uppercase() }
    }
}

