package br.com.rastreadorfrota.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.RegrasFrota
import br.com.rastreadorfrota.ui.components.AvatarUsuario
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.DialogoConfirmacao
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.LinhaDado
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.MotoristaViewModel

private enum class FiltroEquipe(val rotulo: String) { TODOS("Todos"), SEM_VEICULO("Sem veículo"), CNH("CNH a vencer"), INATIVOS("Inativos") }

/**
 * Aba Equipe (gestor). Motoristas criam a própria conta; aqui o gestor atribui o veículo
 * (um por motorista, com confirmação de troca) e ativa/desativa o acesso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipeScreen(
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    viewModel: MotoristaViewModel = viewModel()
) {
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    val veiculos by viewModel.veiculosDisponiveis.collectAsStateWithLifecycle()
    var busca by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf(FiltroEquipe.TODOS) }
    var abertoUid by rememberSaveable { mutableStateOf<String?>(null) }

    val termo = busca.trim().lowercase()
    val lista = motoristas
        .filter {
            when (filtro) {
                FiltroEquipe.TODOS -> true
                FiltroEquipe.SEM_VEICULO -> it.ativo && it.veiculoId == null
                FiltroEquipe.CNH -> (RegrasFrota.diasParaVencerCnh(it.validadeCnh) ?: Long.MAX_VALUE) <= 30
                FiltroEquipe.INATIVOS -> !it.ativo
            }
        }
        .filter { termo.isEmpty() || it.nome.lowercase().contains(termo) || it.email.lowercase().contains(termo) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Espaco.xl),
        verticalArrangement = Arrangement.spacedBy(Espaco.md)
    ) {
        item {
            TopoAba(
                "Equipe",
                usuario,
                onAbrirPerfil,
                subtitulo = "${motoristas.count { it.ativo }} motorista(s) ativo(s)"
            )
            OutlinedTextField(
                value = busca,
                onValueChange = { busca = it },
                placeholder = { Text("Buscar motorista") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Espaco.tela)
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Espaco.tela, vertical = Espaco.sm),
                horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
            ) {
                FiltroEquipe.entries.forEach { f ->
                    FilterChip(
                        selected = filtro == f,
                        onClick = { filtro = f },
                        label = { Text(f.rotulo) },
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
                EstadoVazio(
                    Icons.Filled.Groups,
                    if (motoristas.isEmpty()) "Nenhum motorista ainda" else "Ninguém neste filtro",
                    if (motoristas.isEmpty()) "Peça aos motoristas que criem a conta no app escolhendo \"Motorista\". Eles aparecem aqui depois da sincronização."
                    else "Tente outro filtro ou busca."
                )
            }
        }
        items(lista, key = { it.uid }) { m ->
            CardMotorista(
                motorista = m,
                veiculo = veiculos.find { it.id == m.veiculoId },
                onClick = { abertoUid = m.uid },
                modifier = Modifier.padding(horizontal = Espaco.tela)
            )
        }
    }

    motoristas.find { it.uid == abertoUid }?.let { m ->
        FichaMotorista(
            motorista = m,
            veiculos = veiculos,
            motoristas = motoristas,
            onAtribuir = { viewModel.associarVeiculo(m, it) },
            onAlterarAtivo = { viewModel.alterarAtivo(m, it) },
            onFechar = { abertoUid = null }
        )
    }
}

@Composable
private fun CardMotorista(motorista: MotoristaEntity, veiculo: VeiculoEntity?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dias = RegrasFrota.diasParaVencerCnh(motorista.validadeCnh)
    CardTrak(modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier
                .padding(Espaco.md)
                .semantics(mergeDescendants = true) {
                    contentDescription = "${motorista.nome}, ${if (motorista.ativo) "ativo" else "inativo"}, " +
                        (veiculo?.let { "dirige ${it.placa}" } ?: "sem veículo")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarUsuario(motorista.uid, motorista.nome, 48.dp)
            Spacer(Modifier.width(Espaco.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(motorista.nome, style = MaterialTheme.typography.titleSmall, color = if (motorista.ativo) MaterialTheme.colorScheme.onSurface else TrakSyncTheme.colors.textSecondary)
                    if (!motorista.sincronizado) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.CloudUpload, contentDescription = "Alteração pendente de sincronização", tint = TrakSyncTheme.colors.warning, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!motorista.ativo) StatusPill("Inativo", TrakSyncTheme.colors.textSecondary)
                    else if (dias != null && dias < 0) StatusPill("CNH vencida", MaterialTheme.colorScheme.error, Icons.Filled.Badge)
                    else if (dias != null && dias <= 30) StatusPill("CNH vence em $dias d", TrakSyncTheme.colors.warning, Icons.Filled.Badge)
                    else StatusPill("CNH ${motorista.categoriaCnh.ifBlank { "—" }}", TrakSyncTheme.colors.success, Icons.Filled.Badge)
                }
            }
            if (veiculo != null) PlacaBadge(veiculo.placa, compacta = true)
            else Text("Sem veículo", style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.warning)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FichaMotorista(
    motorista: MotoristaEntity,
    veiculos: List<VeiculoEntity>,
    motoristas: List<MotoristaEntity>,
    onAtribuir: (VeiculoEntity?) -> Unit,
    onAlterarAtivo: (Boolean) -> Unit,
    onFechar: () -> Unit
) {
    val context = LocalContext.current
    var transferir by remember { mutableStateOf<Pair<VeiculoEntity, MotoristaEntity>?>(null) }
    var confirmarDesativar by remember { mutableStateOf(false) }
    val veiculoAtual = veiculos.find { it.id == motorista.veiculoId }

    ModalBottomSheet(onDismissRequest = onFechar, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(bottom = Espaco.xl)) {
            Row(Modifier.padding(horizontal = Espaco.tela), verticalAlignment = Alignment.CenterVertically) {
                AvatarUsuario(motorista.uid, motorista.nome, 56.dp)
                Spacer(Modifier.width(Espaco.md))
                Column(Modifier.weight(1f)) {
                    Text(motorista.nome, style = MaterialTheme.typography.titleMedium)
                    Text(motorista.email, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                }
                Switch(
                    checked = motorista.ativo,
                    onCheckedChange = { ativo -> if (!ativo && motorista.veiculoId != null) confirmarDesativar = true else onAlterarAtivo(ativo) },
                    colors = SwitchDefaults.colors(checkedTrackColor = TrakSyncTheme.colors.success),
                    modifier = Modifier.semantics {
                        contentDescription = "Acesso ao app"
                        stateDescription = if (motorista.ativo) "Ativo" else "Inativo"
                    }
                )
            }
            Column(Modifier.padding(horizontal = Espaco.tela, vertical = Espaco.sm)) {
                LinhaDado("CNH", "${motorista.cnh} · categoria ${motorista.categoriaCnh.ifBlank { "—" }}")
                LinhaDado("Validade", motorista.validadeCnh.ifBlank { "—" })
                if (motorista.telefone.isNotBlank()) LinhaDado("Telefone", motorista.telefone)
            }
            if (motorista.telefone.isNotBlank()) {
                BotaoSecundario(
                    "Ligar para ${motorista.nome.substringBefore(" ")}",
                    Icons.Filled.Phone,
                    { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${motorista.telefone}"))) },
                    Modifier.fillMaxWidth().padding(horizontal = Espaco.tela)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = Espaco.md), color = TrakSyncTheme.colors.surface2)
            Text("Veículo", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = Espaco.tela))
            Text(
                if (motorista.ativo) "Um veículo por motorista. Veículos de outro motorista pedem confirmação."
                else "Ative o motorista para atribuir um veículo.",
                style = MaterialTheme.typography.bodySmall,
                color = TrakSyncTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = Espaco.tela, vertical = Espaco.xs)
            )
            if (motorista.ativo) {
                OpcaoVeiculo("Nenhum", null, veiculoAtual == null, cor = null) { onAtribuir(null) }
                veiculos.forEach { v ->
                    val outro = RegrasFrota.motoristaComVeiculo(motoristas, v.id, motorista.uid)
                    OpcaoVeiculo(
                        "${v.placa} · ${v.modelo}",
                        outro?.let { "Com ${it.nome.substringBefore(" ")}" } ?: "Livre",
                        v.id == motorista.veiculoId,
                        cor = if (outro != null) TrakSyncTheme.colors.warning else TrakSyncTheme.colors.success
                    ) {
                        if (outro != null) transferir = v to outro else if (v.id != motorista.veiculoId) onAtribuir(v)
                    }
                }
                if (veiculos.isEmpty()) {
                    Text(
                        "Nenhum veículo aprovado na frota.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrakSyncTheme.colors.textSecondary,
                        modifier = Modifier.padding(Espaco.tela)
                    )
                }
            }
        }
    }

    transferir?.let { (v, outro) ->
        DialogoConfirmacao(
            titulo = "Transferir ${v.placa}?",
            mensagem = "${v.placa} está com ${outro.nome.substringBefore(" ")}. Ao confirmar, ele passa para ${motorista.nome.substringBefore(" ")} " +
                "e ${outro.nome.substringBefore(" ")} fica sem veículo.",
            confirmar = "Transferir",
            icone = Icons.Filled.SwapHoriz,
            onConfirmar = { transferir = null; onAtribuir(v) },
            onCancelar = { transferir = null }
        )
    }
    if (confirmarDesativar) {
        DialogoConfirmacao(
            titulo = "Desativar ${motorista.nome.substringBefore(" ")}?",
            mensagem = "Ele perde o acesso ao app e o veículo ${veiculoAtual?.placa.orEmpty()} fica livre para outro motorista.",
            confirmar = "Desativar",
            destrutivo = true,
            onConfirmar = { confirmarDesativar = false; onAlterarAtivo(false) },
            onCancelar = { confirmarDesativar = false }
        )
    }
}

@Composable
private fun OpcaoVeiculo(titulo: String, detalhe: String?, selecionado: Boolean, cor: Color?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Espaco.tela, vertical = Espaco.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selecionado) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selecionado) MaterialTheme.colorScheme.primary else TrakSyncTheme.colors.textSecondary
        )
        Spacer(Modifier.width(Espaco.md))
        Text(titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (detalhe != null && cor != null) Text(detalhe, style = MaterialTheme.typography.labelMedium, color = cor)
    }
}
