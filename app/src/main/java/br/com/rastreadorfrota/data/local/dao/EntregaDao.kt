package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntregaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entrega: EntregaEntity): Long

    @Update
    suspend fun atualizar(entrega: EntregaEntity)

    @Query("SELECT * FROM entregas WHERE deletedLocally = 0 ORDER BY inicioEm DESC")
    fun observarTodas(): Flow<List<EntregaEntity>>

    @Query("SELECT * FROM entregas WHERE deletedLocally = 0 AND placa = :placa ORDER BY inicioEm DESC")
    fun observarDoVeiculo(placa: String): Flow<List<EntregaEntity>>

    @Query("SELECT * FROM entregas WHERE id = :id")
    fun observar(id: Long): Flow<EntregaEntity?>

    @Query("SELECT * FROM entregas WHERE id = :id")
    suspend fun buscarPorId(id: Long): EntregaEntity?

    @Query("SELECT * FROM entregas WHERE remoteId = :remoteId LIMIT 1")
    suspend fun buscarPorRemoteId(remoteId: String): EntregaEntity?

    @Query("SELECT * FROM entregas WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<EntregaEntity>

    @Query("DELETE FROM entregas WHERE id = :id")
    suspend fun excluirDefinitivo(id: Long)
}
