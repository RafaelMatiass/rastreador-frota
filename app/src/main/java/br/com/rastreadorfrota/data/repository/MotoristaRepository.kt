package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

class MotoristaRepository(private val dao: MotoristaDao) {
    val motoristas: Flow<List<MotoristaEntity>> = dao.observarTodos()

    suspend fun salvar(motorista: MotoristaEntity) = dao.inserir(motorista)
    suspend fun atualizar(motorista: MotoristaEntity) = dao.atualizar(motorista)
    suspend fun remover(motorista: MotoristaEntity) = dao.deletar(motorista)
}