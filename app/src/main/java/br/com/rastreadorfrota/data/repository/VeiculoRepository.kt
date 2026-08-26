package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(private val dao: VeiculoDao) {
    val veiculos: Flow<List<VeiculoEntity>> = dao.observarTodos()

    suspend fun salvar(veiculo: VeiculoEntity) = dao.inserir(veiculo)
    suspend fun atualizar(veiculo: VeiculoEntity) = dao.atualizar(veiculo)
    suspend fun remover(veiculo: VeiculoEntity) = dao.deletar(veiculo)
}