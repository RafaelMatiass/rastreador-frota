package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM motoristas WHERE uid = :uid")
    suspend fun buscarPorUid(uid: String): MotoristaEntity?

    @Query("SELECT * FROM motoristas WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<MotoristaEntity>

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
}
