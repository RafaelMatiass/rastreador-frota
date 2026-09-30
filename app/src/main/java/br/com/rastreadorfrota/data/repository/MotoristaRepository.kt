package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

/**
 * Motoristas se cadastram sozinhos pelo app; o controlador só associa
 * veículo e ativa/desativa. Por isso não existe salvar/remover aqui.
 */
class MotoristaRepository(private val dao: MotoristaDao) {
    val motoristas: Flow<List<MotoristaEntity>> = dao.observarTodos()

    suspend fun associarVeiculo(motorista: MotoristaEntity, veiculoId: Long?) =
        dao.atualizar(motorista.copy(veiculoId = veiculoId, sincronizado = false))

    suspend fun alterarAtivo(motorista: MotoristaEntity, ativo: Boolean) =
        dao.atualizar(motorista.copy(ativo = ativo, sincronizado = false))
}
