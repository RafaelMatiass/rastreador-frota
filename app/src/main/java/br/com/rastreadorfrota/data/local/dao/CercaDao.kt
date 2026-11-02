package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CercaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(cerca: CercaEntity): Long

    @Update
    suspend fun atualizar(cerca: CercaEntity)

    @Query("SELECT * FROM cercas WHERE deletedLocally = 0 ORDER BY nome ASC")
    fun observarTodas(): Flow<List<CercaEntity>>

    @Query("SELECT * FROM cercas WHERE id = :id")
    suspend fun buscarPorId(id: Long): CercaEntity?

    @Query("SELECT * FROM cercas WHERE remoteId = :remoteId LIMIT 1")
    suspend fun buscarPorRemoteId(remoteId: String): CercaEntity?

    @Query("SELECT * FROM cercas WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<CercaEntity>

    @Query("SELECT COUNT(*) FROM cercas WHERE sincronizado = 0")
    fun observarQuantidadePendentes(): Flow<Int>

    @Query("DELETE FROM cercas WHERE id = :id")
    suspend fun excluirDefinitivo(id: Long)
}
