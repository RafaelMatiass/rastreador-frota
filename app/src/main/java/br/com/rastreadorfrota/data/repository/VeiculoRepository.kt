package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

class VeiculoRepository(private val dao: VeiculoDao) {
    val veiculos: Flow<List<VeiculoEntity>> = dao.observarTodos()

    fun observar(id: Long): Flow<VeiculoEntity?> = dao.observarPorId(id)

    suspend fun buscar(id: Long): VeiculoEntity? = dao.buscarPorId(id)

    /** Outra placa igual já cadastrada (ignora o próprio veículo na edição). */
    suspend fun placaEmUso(placa: String, idAtual: Long = 0): Boolean =
        dao.buscarPorPlaca(placa)?.let { it.id != idAtual } ?: false

    suspend fun salvar(veiculo: VeiculoEntity) =
        dao.inserir(veiculo.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    suspend fun atualizar(veiculo: VeiculoEntity) =
        dao.atualizar(veiculo.copy(sincronizado = false, updatedAt = System.currentTimeMillis()))

    suspend fun alterarSituacao(veiculo: VeiculoEntity, situacao: SituacaoVeiculo) =
        atualizar(veiculo.copy(situacao = situacao.name, ativo = situacao == SituacaoVeiculo.ATIVO))

    // Soft delete: só marca. A exclusão de verdade (local + Firestore)
    // acontece dentro do FirestoreSyncManager, depois de confirmar a nuvem.
    suspend fun remover(veiculo: VeiculoEntity) =
        atualizar(veiculo.copy(deletedLocally = true))

    /** "Desfazer" da exclusão: enquanto não sincronizou, basta desmarcar. */
    suspend fun desfazerRemocao(veiculo: VeiculoEntity) =
        atualizar(veiculo.copy(deletedLocally = false))
}
