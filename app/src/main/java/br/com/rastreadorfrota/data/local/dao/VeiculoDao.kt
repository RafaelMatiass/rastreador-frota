package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(veiculo: VeiculoEntity): Long

    @Update
    suspend fun atualizar(veiculo: VeiculoEntity)

    @Delete
    suspend fun deletar(veiculo: VeiculoEntity)

    // Registros com deletedLocally não aparecem mais pra UI, mesmo que ainda
    // existam no banco esperando a sincronização confirmar a exclusão.
    @Query("SELECT * FROM veiculos WHERE deletedLocally = 0 ORDER BY placa ASC")
    fun observarTodos(): Flow<List<VeiculoEntity>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: Long): VeiculoEntity?

    @Query("SELECT * FROM veiculos WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<VeiculoEntity>

    @Query("SELECT * FROM veiculos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun buscarPorRemoteId(remoteId: String): VeiculoEntity?

    @Query("DELETE FROM veiculos WHERE id = :id")
    suspend fun excluirDefinitivo(id: Long)
}