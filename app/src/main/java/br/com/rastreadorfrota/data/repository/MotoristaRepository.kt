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

    /** Um veículo, um motorista: quem estava com ele fica sem (ver [MotoristaDao.atribuirExclusivo]). */
    suspend fun associarVeiculo(motorista: MotoristaEntity, veiculoId: Long?) =
        dao.atribuirExclusivo(motorista.uid, veiculoId)

    /** Desativado, o motorista perde o acesso e o veículo dele fica livre. */
    suspend fun alterarAtivo(motorista: MotoristaEntity, ativo: Boolean) =
        if (ativo) dao.definirAtivo(motorista.uid, true) else dao.desativarLiberandoVeiculo(motorista.uid)

    /** Veículo excluído ou desativado não pode continuar com motorista. */
    suspend fun liberarVeiculo(veiculoId: Long) = dao.soltarVeiculo(veiculoId)
}
