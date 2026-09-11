package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.MotoristaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroMotoristaScreen(
    onBack: () -> Unit,
    viewModel: MotoristaViewModel = viewModel()
) {
    val motoristas by viewModel.motoristas.collectAsState()
    val veiculos by viewModel.veiculosDisponiveis.collectAsState()

    var nome by remember { mutableStateOf("") }
    var cnh by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var veiculoSelecionado by remember { mutableStateOf<VeiculoEntity?>(null) }
    var expandedVeiculo by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Motoristas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
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
                    "Cadastrar motorista",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                OutlinedTextField(
                    value = nome, onValueChange = { nome = it },
                    label = { Text("Nome") }, singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = cnh, onValueChange = { cnh = it },
                    label = { Text("CNH") }, singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = telefone, onValueChange = { telefone = it },
                    label = { Text("Telefone") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Email") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedVeiculo,
                    onExpandedChange = { expandedVeiculo = it }
                ) {
                    OutlinedTextField(
                        value = veiculoSelecionado?.placa ?: "Nenhum veículo associado",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Veículo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVeiculo) },
                        shape = MaterialTheme.shapes.medium,
                        colors = trakSyncTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedVeiculo,
                        onDismissRequest = { expandedVeiculo = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhum") },
                            onClick = { veiculoSelecionado = null; expandedVeiculo = false }
                        )
                        veiculos.forEach { veiculo ->
                            DropdownMenuItem(
                                text = { Text("${veiculo.placa} - ${veiculo.modelo}") },
                                onClick = { veiculoSelecionado = veiculo; expandedVeiculo = false }
                            )
                        }
                    }
                }
            }
            erro?.let { mensagem ->
                item { Text(mensagem, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        if (nome.isBlank() || cnh.isBlank()) {
                            erro = "Preencha nome e CNH."
                            return@Button
                        }
                        erro = null
                        viewModel.salvarMotorista(nome, cnh, telefone, email, veiculoSelecionado?.id)
                        nome = ""; cnh = ""; telefone = ""; email = ""; veiculoSelecionado = null
                    },
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncPrimaryButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Salvar motorista", style = MaterialTheme.typography.labelLarge)
                }
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = TrakSyncTheme.colors.surface2)
                Text(
                    "Motoristas cadastrados (${motoristas.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (motoristas.isEmpty()) {
                item {
                    Text("Nenhum motorista cadastrado ainda.", color = TrakSyncTheme.colors.textSecondary)
                }
            }

            items(motoristas, key = { it.id }) { motorista ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                motorista.nome,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(motorista.cnh, color = TrakSyncTheme.colors.textSecondary)
                            val placaVeiculo = veiculos.find { it.id == motorista.veiculoId }?.placa
                            Text(
                                placaVeiculo?.let { "Veículo: $it" } ?: "Sem veículo associado",
                                style = MaterialTheme.typography.bodySmall,
                                color = TrakSyncTheme.colors.textSecondary
                            )
                        }
                        IconButton(onClick = { viewModel.removerMotorista(motorista) }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remover",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}