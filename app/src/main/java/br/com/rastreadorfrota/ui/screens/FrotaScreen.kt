package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel

private enum class FiltroFrota(val rotulo: String) { TODOS("Todos"), ATIVOS("Ativos"), PENDENTES("Pendentes"), SEM_MOTORISTA("Sem motorista"), INATIVOS("Inativos") }

/** Aba Frota (gestor): todos os veículos, com busca, filtros e aprovação direto na lista. */
@Composable
fun FrotaScreen(
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    onNovoVeiculo: () -> Unit,
    onAbrirVeiculo: (Long) -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroFrota.TODOS) }

    val motoristaPorVeiculo = motoristas.filter { it.veiculoId != null }.associateBy { it.veiculoId }
    val termo = busca.trim().lowercase()
    val lista = veiculos
        .filter { v ->
            when (filtro) {
                FiltroFrota.TODOS -> true
                FiltroFrota.ATIVOS -> v.situacaoEnum == SituacaoVeiculo.ATIVO
                FiltroFrota.PENDENTES -> v.situacaoEnum == SituacaoVeiculo.PENDENTE
                FiltroFrota.SEM_MOTORISTA -> v.situacaoEnum == SituacaoVeiculo.ATIVO && motoristaPorVeiculo[v.id] == null
                FiltroFrota.INATIVOS -> v.situacaoEnum == SituacaoVeiculo.INATIVO
            }
        }
        .filter { v ->
            termo.isEmpty() || v.placa.lowercase().contains(termo) || v.modelo.lowercase().contains(termo) ||
                motoristaPorVeiculo[v.id]?.nome?.lowercase()?.contains(termo) == true
        }
        // Pendentes primeiro: são o que pede ação do gestor.
        .sortedWith(compareBy<VeiculoEntity> { it.situacaoEnum != SituacaoVeiculo.PENDENTE }.thenBy { it.placa })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(Espaco.md)
        ) {
            item {
                TopoAba("Frota", usuario, onAbrirPerfil, subtitulo = "${veiculos.size} veículo(s) cadastrado(s)")
                OutlinedTextField(
                    value = busca,
                    onValueChange = { busca = it },
                    placeholder = { Text("Buscar por placa, modelo ou motorista") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Espaco.tela)
                )
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Espaco.tela, vertical = Espaco.sm),
                    horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
                ) {
                    FiltroFrota.entries.forEach { f ->
                        val n = when (f) {
                            FiltroFrota.PENDENTES -> veiculos.count { it.situacaoEnum == SituacaoVeiculo.PENDENTE }
                            else -> null
                        }
                        FilterChip(
                            selected = filtro == f,
                            onClick = { filtro = f },
                            label = { Text(if (n != null && n > 0) "${f.rotulo} ($n)" else f.rotulo) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (lista.isEmpty()) {
                item {
                    if (veiculos.isEmpty()) {
                        EstadoVazio(
                            Icons.Filled.LocalShipping,
                            "Sua frota ainda está vazia",
                            "Cadastre o primeiro veículo ou peça aos motoristas que cadastrem os deles para você aprovar.",
                            acao = "Cadastrar veículo",
                            onAcao = onNovoVeiculo
                        )
                    } else {
                        EstadoVazio(Icons.Filled.SearchOff, "Nada encontrado", "Nenhum veículo corresponde à busca e ao filtro escolhidos.")
                    }
                }
            }

            items(lista, key = { it.id }) { veiculo ->
                CardVeiculoFrota(
                    veiculo = veiculo,
                    motorista = motoristaPorVeiculo[veiculo.id],
                    onClick = { onAbrirVeiculo(veiculo.id) },
                    onAprovar = { viewModel.aprovar(veiculo) },
                    modifier = Modifier.padding(horizontal = Espaco.tela)
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNovoVeiculo,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Novo veículo") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Espaco.lg)
        )
    }
}

@Composable
fun CardVeiculoFrota(
    veiculo: VeiculoEntity,
    motorista: MotoristaEntity?,
    onClick: () -> Unit,
    onAprovar: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val tipo = tipoVeiculo(veiculo.tipo)
    val pendente = veiculo.situacaoEnum == SituacaoVeiculo.PENDENTE
    CardTrak(modifier.fillMaxWidth(), onClick = onClick) {
        Column(Modifier.padding(Espaco.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${veiculo.placa}, ${veiculo.modelo}, ${tipo.label}, ${veiculo.situacaoEnum.label}" +
                        (motorista?.let { ", com ${it.nome}" } ?: ", sem motorista")
                }
            ) {
                FotoVeiculo(veiculo.fotoLocalPath, tipo.icone, Modifier.size(64.dp))
                Spacer(Modifier.width(Espaco.md))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlacaBadge(veiculo.placa, compacta = true)
                        if (!veiculo.sincronizado) {
                            Spacer(Modifier.width(Espaco.sm))
                            Icon(Icons.Filled.CloudUpload, contentDescription = "Aguardando sincronizar", tint = TrakSyncTheme.colors.warning, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        "${veiculo.modelo} · ${tipo.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = TrakSyncTheme.colors.textSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            motorista?.nome ?: "Sem motorista",
                            style = MaterialTheme.typography.bodySmall,
                            color = TrakSyncTheme.colors.textSecondary,
                            maxLines = 1
                        )
                    }
                }
                StatusPill(veiculo.situacaoEnum)
            }
            if (pendente && onAprovar != null) {
                Row(Modifier.fillMaxWidth().padding(top = Espaco.sm), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onAprovar) { Text("Aprovar veículo") }
                }
            }
        }
    }
}
