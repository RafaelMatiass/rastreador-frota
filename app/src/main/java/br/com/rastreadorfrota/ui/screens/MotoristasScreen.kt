package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.MotoristaViewModel

/**
 * Gestão de motoristas pelo controlador. Não há formulário de cadastro:
 * o motorista cria a própria conta no app e aparece aqui após sincronizar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotoristasScreen(
    onBack: () -> Unit,
    viewModel: MotoristaViewModel = viewModel()
) {
    val motoristas by viewModel.motoristas.collectAsState()
    val veiculos by viewModel.veiculosDisponiveis.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Motoristas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Motoristas cadastrados (${motoristas.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Os motoristas criam a própria conta no app. Aqui você associa o veículo e ativa ou desativa o acesso.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TrakSyncTheme.colors.textSecondary
                )
            }

            if (motoristas.isEmpty()) {
                item {
                    Text(
                        "Nenhum motorista ainda. Eles aparecem aqui depois de criar a conta com o perfil Motorista.",
                        color = TrakSyncTheme.colors.textSecondary
                    )
                }
            }

            items(motoristas, key = { it.uid }) { motorista ->
                MotoristaCard(
                    motorista = motorista,
                    veiculos = veiculos,
                    motoristas = motoristas,
                    onAssociarVeiculo = { viewModel.associarVeiculo(motorista, it) },
                    onAlterarAtivo = { viewModel.alterarAtivo(motorista, it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotoristaCard(
    motorista: MotoristaEntity,
    veiculos: List<VeiculoEntity>,
    motoristas: List<MotoristaEntity>,
    onAssociarVeiculo: (Long?) -> Unit,
    onAlterarAtivo: (Boolean) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    val veiculoAtual = veiculos.find { it.id == motorista.veiculoId }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (motorista.ativo) 1f else 0.5f)
                ) {
                    Text(
                        motorista.nome,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(motorista.email, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                    Text(
                        "CNH ${motorista.cnh}" +
                            motorista.categoriaCnh.takeIf { it.isNotBlank() }?.let { " · Cat. $it" }.orEmpty() +
                            motorista.validadeCnh.takeIf { it.isNotBlank() }?.let { " · Validade $it" }.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TrakSyncTheme.colors.textSecondary
                    )
                    if (motorista.telefone.isNotBlank()) {
                        Text(motorista.telefone, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Switch(
                        checked = motorista.ativo,
                        onCheckedChange = onAlterarAtivo,
                        colors = SwitchDefaults.colors(checkedTrackColor = TrakSyncTheme.colors.success)
                    )
                    Text(
                        if (motorista.ativo) "Ativo" else "Inativo",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (motorista.ativo) TrakSyncTheme.colors.success else TrakSyncTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ExposedDropdownMenuBox(
                expanded = expandido,
                onExpandedChange = { expandido = it }
            ) {
                OutlinedTextField(
                    value = veiculoAtual?.let { "${it.placa} - ${it.modelo}" } ?: "Nenhum veículo associado",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Veículo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                )
                ExposedDropdownMenu(
                    expanded = expandido,
                    onDismissRequest = { expandido = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Nenhum") },
                        onClick = { onAssociarVeiculo(null); expandido = false }
                    )
                    veiculos.forEach { veiculo ->
                        // Mostra quem já usa o veículo, sem impedir o compartilhamento.
                        val outro = motoristas.firstOrNull { it.veiculoId == veiculo.id && it.uid != motorista.uid }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${veiculo.placa} - ${veiculo.modelo}" +
                                        outro?.let { " (com ${it.nome.substringBefore(" ")})" }.orEmpty()
                                )
                            },
                            onClick = { onAssociarVeiculo(veiculo.id); expandido = false }
                        )
                    }
                }
            }

            if (!motorista.sincronizado) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        tint = TrakSyncTheme.colors.warning,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        "Alteração pendente de sincronização",
                        style = MaterialTheme.typography.labelSmall,
                        color = TrakSyncTheme.colors.warning
                    )
                }
            }
        }
    }
}
