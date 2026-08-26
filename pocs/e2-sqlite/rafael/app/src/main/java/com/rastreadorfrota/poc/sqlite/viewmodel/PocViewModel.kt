package com.rastreadorfrota.poc.sqlite.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rastreadorfrota.poc.sqlite.data.AppDatabase
import com.rastreadorfrota.poc.sqlite.data.entity.Motorista
import com.rastreadorfrota.poc.sqlite.data.entity.Negociante
import com.rastreadorfrota.poc.sqlite.data.entity.OcorrenciaEntrega
import com.rastreadorfrota.poc.sqlite.data.entity.Produto
import com.rastreadorfrota.poc.sqlite.data.entity.Veiculo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// AndroidViewModel (em vez de ViewModel puro) porque o Room precisa do
// Context da aplicação pra abrir/criar o banco.
class PocViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    // stateIn converte o Flow do Room em StateFlow: a UI sempre tem um
    // valor pra ler de cara (lista vazia) em vez de esperar a primeira
    // emissão, e WhileSubscribed(5000) mantém a coleta viva por 5s após a
    // tela sair de composição, evitando reabrir a query à toa em rotações.
    val produtos = db.produtoDao().listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val negociantes = db.negocianteDao().listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val motoristas = db.motoristaDao().listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veiculos = db.veiculoDao().listarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ocorrencias = db.ocorrenciaEntregaDao().listarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun salvarProduto(nome: String, descricao: String, pesoKg: Double, quantidade: Int) {
        viewModelScope.launch {
            db.produtoDao().inserir(
                Produto(nome = nome, descricao = descricao, pesoKg = pesoKg, quantidade = quantidade)
            )
        }
    }

    fun salvarNegociante(nome: String, cnpjCpf: String, telefone: String, endereco: String) {
        viewModelScope.launch {
            db.negocianteDao().inserir(
                Negociante(nome = nome, cnpjCpf = cnpjCpf, telefone = telefone, endereco = endereco)
            )
        }
    }

    fun salvarMotorista(nome: String, cnh: String, telefone: String) {
        viewModelScope.launch {
            db.motoristaDao().inserir(Motorista(nome = nome, cnh = cnh, telefone = telefone))
        }
    }

    fun salvarVeiculo(placa: String, modelo: String, capacidadeCargaKg: Double, motoristaId: Long?) {
        viewModelScope.launch {
            db.veiculoDao().inserir(
                Veiculo(
                    placa = placa,
                    modelo = modelo,
                    capacidadeCargaKg = capacidadeCargaKg,
                    motoristaId = motoristaId
                )
            )
        }
    }

    fun salvarOcorrencia(produtoId: Long, veiculoId: Long?, status: String, observacao: String) {
        viewModelScope.launch {
            db.ocorrenciaEntregaDao().inserir(
                OcorrenciaEntrega(
                    produtoId = produtoId,
                    veiculoId = veiculoId,
                    status = status,
                    observacao = observacao
                )
            )
        }
    }
}
