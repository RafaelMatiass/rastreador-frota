package com.rastreadorfrota.poc.sqlite.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rastreadorfrota.poc.sqlite.viewmodel.PocViewModel

private val abas = listOf("Produto", "Negociante", "Motorista", "Veículo", "Ocorrência")

@Composable
fun PocScreen(viewModel: PocViewModel = viewModel()) {
    var abaSelecionada by remember { mutableIntStateOf(0) }

    Scaffold { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = abaSelecionada) {
                abas.forEachIndexed { index, titulo ->
                    Tab(
                        selected = abaSelecionada == index,
                        onClick = { abaSelecionada = index },
                        text = { Text(titulo) }
                    )
                }
            }

            when (abaSelecionada) {
                0 -> ProdutoTab(viewModel)
                1 -> NegocianteTab(viewModel)
                2 -> MotoristaTab(viewModel)
                3 -> VeiculoTab(viewModel)
                4 -> OcorrenciaTab(viewModel)
            }
        }
    }
}

@Composable
private fun ProdutoTab(viewModel: PocViewModel) {
    var nome by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf("") }
    val lista by viewModel.produtos.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome do produto") }, modifier = Modifier.fillMaxWidth())
        TextField(value = descricao, onValueChange = { descricao = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth())
        TextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth())
        TextField(value = quantidade, onValueChange = { quantidade = it }, label = { Text("Quantidade") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            viewModel.salvarProduto(nome, descricao, peso.toDoubleOrNull() ?: 0.0, quantidade.toIntOrNull() ?: 0)
            nome = ""; descricao = ""; peso = ""; quantidade = ""
        }) { Text("Salvar produto") }

        Text("Produtos salvos (${lista.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lista) { p ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("#${p.id} ${p.nome} — ${p.quantidade}un — ${p.pesoKg}kg", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun NegocianteTab(viewModel: PocViewModel) {
    var nome by remember { mutableStateOf("") }
    var cnpjCpf by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    val lista by viewModel.negociantes.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        TextField(value = cnpjCpf, onValueChange = { cnpjCpf = it }, label = { Text("CNPJ/CPF") }, modifier = Modifier.fillMaxWidth())
        TextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
        TextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            viewModel.salvarNegociante(nome, cnpjCpf, telefone, endereco)
            nome = ""; cnpjCpf = ""; telefone = ""; endereco = ""
        }) { Text("Salvar negociante") }

        Text("Negociantes salvos (${lista.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lista) { n ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("#${n.id} ${n.nome} — ${n.cnpjCpf}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MotoristaTab(viewModel: PocViewModel) {
    var nome by remember { mutableStateOf("") }
    var cnh by remember { mutableStateOf("") }
    var telefone by remember { mutableStateOf("") }
    val lista by viewModel.motoristas.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
        TextField(value = cnh, onValueChange = { cnh = it }, label = { Text("CNH") }, modifier = Modifier.fillMaxWidth())
        TextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            viewModel.salvarMotorista(nome, cnh, telefone)
            nome = ""; cnh = ""; telefone = ""
        }) { Text("Salvar motorista") }

        Text("Motoristas salvos (${lista.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lista) { m ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("#${m.id} ${m.nome} — CNH ${m.cnh}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun VeiculoTab(viewModel: PocViewModel) {
    var placa by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var capacidade by remember { mutableStateOf("") }
    var motoristaId by remember { mutableStateOf("") }
    val lista by viewModel.veiculos.collectAsState()
    val motoristas by viewModel.motoristas.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") }, modifier = Modifier.fillMaxWidth())
        TextField(value = modelo, onValueChange = { modelo = it }, label = { Text("Modelo") }, modifier = Modifier.fillMaxWidth())
        TextField(value = capacidade, onValueChange = { capacidade = it }, label = { Text("Capacidade de carga (kg)") }, modifier = Modifier.fillMaxWidth())
        TextField(
            value = motoristaId,
            onValueChange = { motoristaId = it },
            label = { Text("ID do motorista (opcional, veja aba Motorista)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            viewModel.salvarVeiculo(placa, modelo, capacidade.toDoubleOrNull() ?: 0.0, motoristaId.toLongOrNull())
            placa = ""; modelo = ""; capacidade = ""; motoristaId = ""
        }) { Text("Salvar veículo") }

        if (motoristas.isNotEmpty()) {
            Text("IDs disponíveis: ${motoristas.joinToString { "#${it.id} ${it.nome}" }}", style = MaterialTheme.typography.bodySmall)
        }

        Text("Veículos salvos (${lista.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lista) { v ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("#${v.id} ${v.placa} (${v.modelo}) — motoristaId=${v.motoristaId}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun OcorrenciaTab(viewModel: PocViewModel) {
    var produtoId by remember { mutableStateOf("") }
    var veiculoId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var observacao by remember { mutableStateOf("") }
    val lista by viewModel.ocorrencias.collectAsState()
    val produtos by viewModel.produtos.collectAsState()
    val veiculos by viewModel.veiculos.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(value = produtoId, onValueChange = { produtoId = it }, label = { Text("ID do produto") }, modifier = Modifier.fillMaxWidth())
        TextField(value = veiculoId, onValueChange = { veiculoId = it }, label = { Text("ID do veículo (opcional)") }, modifier = Modifier.fillMaxWidth())
        TextField(value = status, onValueChange = { status = it }, label = { Text("Status (EM_TRANSITO/ENTREGUE/ATRASADO/AVARIADO)") }, modifier = Modifier.fillMaxWidth())
        TextField(value = observacao, onValueChange = { observacao = it }, label = { Text("Observação") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val pid = produtoId.toLongOrNull()
            if (pid != null) {
                viewModel.salvarOcorrencia(pid, veiculoId.toLongOrNull(), status.ifBlank { "EM_TRANSITO" }, observacao)
                produtoId = ""; veiculoId = ""; status = ""; observacao = ""
            }
        }) { Text("Salvar ocorrência") }

        if (produtos.isNotEmpty()) {
            Text("Produtos: ${produtos.joinToString { "#${it.id} ${it.nome}" }}", style = MaterialTheme.typography.bodySmall)
        }
        if (veiculos.isNotEmpty()) {
            Text("Veículos: ${veiculos.joinToString { "#${it.id} ${it.placa}" }}", style = MaterialTheme.typography.bodySmall)
        }

        Text("Ocorrências salvas (${lista.size})", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(lista) { o ->
                Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                    Text("#${o.id} produto=${o.produtoId} veiculo=${o.veiculoId} — ${o.status}", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}
