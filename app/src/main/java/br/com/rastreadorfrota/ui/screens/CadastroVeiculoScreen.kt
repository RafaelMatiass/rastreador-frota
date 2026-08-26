package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroVeiculoScreen(
    onBack: () -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsState()

    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var tipoSelecionado by remember { mutableStateOf(TipoVeiculo.CAMINHAO) }
    var capacidadeTexto by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Veículos da frota") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Cadastrar veículo", style = MaterialTheme.typography.titleMedium) }

            item {
                OutlinedTextField(
                    value = placa,
                    onValueChange = { placa = it },
                    label = { Text("Placa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = modelo,
                    onValueChange = { modelo = it },
                    label = { Text("Modelo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedTipo,
                    onExpandedChange = { expandedTipo = it }
                ) {
                    OutlinedTextField(
                        value = tipoSelecionado.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false }
                    ) {
                        TipoVeiculo.entries.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.label) },
                                onClick = {
                                    tipoSelecionado = tipo
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = capacidadeTexto,
                    onValueChange = { capacidadeTexto = it },
                    label = { Text("Capacidade de carga (kg) - opcional") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            erro?.let { mensagem ->
                item { Text(mensagem, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = {
                        if (placa.isBlank() || modelo.isBlank()) {
                            erro = "Preencha placa e modelo."
                            return@Button
                        }
                        erro = null
                        viewModel.salvarVeiculo(
                            placa = placa,
                            modelo = modelo,
                            tipo = tipoSelecionado.name,
                            capacidadeCargaKg = capacidadeTexto.toDoubleOrNull()
                        )
                        placa = ""
                        modelo = ""
                        capacidadeTexto = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar veículo")
                }
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Veículos cadastrados (${veiculos.size})", style = MaterialTheme.typography.titleMedium)
            }

            if (veiculos.isEmpty()) {
                item { Text("Nenhum veículo cadastrado ainda.") }
            }

            items(veiculos, key = { it.id }) { veiculo ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Column {
                            Text(veiculo.placa, style = MaterialTheme.typography.titleSmall)
                            Text("${veiculo.modelo} · ${TipoVeiculo.valueOf(veiculo.tipo).label}")
                            veiculo.capacidadeCargaKg?.let {
                                Text("Capacidade: $it kg", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { viewModel.removerVeiculo(veiculo) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remover")
                        }
                    }
                }
            }
        }
    }
}