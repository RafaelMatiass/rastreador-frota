package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoristaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(motorista: MotoristaEntity)

    @Update
    suspend fun atualizar(motorista: MotoristaEntity)

    @Query("SELECT * FROM motoristas ORDER BY ativo DESC, nome ASC")
    fun observarTodos(): Flow<List<MotoristaEntity>>

    @Query("SELECT * FROM motoristas ORDER BY ativo DESC, nome ASC")
    suspend fun listarTodos(): List<MotoristaEntity>

    @Query("SELECT * FROM motoristas WHERE uid = :uid")
    suspend fun buscarPorUid(uid: String): MotoristaEntity?

    @Query("SELECT * FROM motoristas WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<MotoristaEntity>

    @Query("SELECT COUNT(*) FROM motoristas WHERE sincronizado = 0")
    fun observarQuantidadePendentes(): Flow<Int>

    // Só marca como sincronizado se o valor ainda é o que foi enviado: se o
    // controlador mudou de novo durante o envio, continua pendente.
    @Query(
        "UPDATE motoristas SET sincronizado = 1 " +
            "WHERE uid = :uid AND veiculoId IS :veiculoId AND ativo = :ativo"
    )
    suspend fun marcarSincronizado(uid: String, veiculoId: Long?, ativo: Boolean)

    // Contas de motorista que não existem mais na nuvem somem do cache.
    @Query("DELETE FROM motoristas WHERE sincronizado = 1 AND uid NOT IN (:uidsNaNuvem)")
    suspend fun removerAusentes(uidsNaNuvem: List<String>): Int

    @Query("UPDATE motoristas SET veiculoId = NULL, sincronizado = 0 WHERE veiculoId = :veiculoId AND uid != :exceto")
    suspend fun soltarVeiculo(veiculoId: Long, exceto: String = "")

    @Query("UPDATE motoristas SET veiculoId = :veiculoId, sincronizado = 0 WHERE uid = :uid")
    suspend fun definirVeiculo(uid: String, veiculoId: Long?)

    @Query("UPDATE motoristas SET ativo = :ativo, sincronizado = 0 WHERE uid = :uid")
    suspend fun definirAtivo(uid: String, ativo: Boolean)

    /**
     * Um veículo, um motorista: quem estava com o veículo fica sem ele, na mesma transação.
     * As duas alterações ficam pendentes e sobem juntas na próxima sincronização.
     */
    @Transaction
    suspend fun atribuirExclusivo(uid: String, veiculoId: Long?) {
        if (veiculoId != null) soltarVeiculo(veiculoId, exceto = uid)
        definirVeiculo(uid, veiculoId)
    }

    /** Desativar o motorista libera o veículo dele para outro. */
    @Transaction
    suspend fun desativarLiberandoVeiculo(uid: String) {
        definirVeiculo(uid, null)
        definirAtivo(uid, false)
    }
}
