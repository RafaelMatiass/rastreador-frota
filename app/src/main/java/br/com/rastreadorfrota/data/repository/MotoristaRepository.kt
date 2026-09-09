package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

class MotoristaRepository(private val dao: MotoristaDao) {
    val motoristas: Flow<List<MotoristaEntity>> = dao.observarTodos()

    suspend fun salvar(motorista: MotoristaEntity) =
        dao.inserir(motorista.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    suspend fun atualizar(motorista: MotoristaEntity) =
        dao.atualizar(motorista.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    suspend fun remover(motorista: MotoristaEntity) =
        dao.atualizar(
            motorista.copy(
                deletedLocally = true,
                sincronizado = false,
                updatedAt = System.currentTimeMillis()
            )
        )
}