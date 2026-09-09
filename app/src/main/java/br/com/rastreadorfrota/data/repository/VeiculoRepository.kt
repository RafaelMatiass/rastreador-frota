package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(private val dao: VeiculoDao) {
    val veiculos: Flow<List<VeiculoEntity>> = dao.observarTodos()

    suspend fun salvar(veiculo: VeiculoEntity) =
        dao.inserir(veiculo.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    suspend fun atualizar(veiculo: VeiculoEntity) =
        dao.atualizar(veiculo.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    // Soft delete: só marca. A exclusão de verdade (local + Firestore)
    // acontece dentro do FirestoreSyncManager, depois de confirmar a nuvem.
    suspend fun remover(veiculo: VeiculoEntity) =
        dao.atualizar(
            veiculo.copy(
                deletedLocally = true,
                sincronizado = false,
                updatedAt = System.currentTimeMillis()
            )
        )
}