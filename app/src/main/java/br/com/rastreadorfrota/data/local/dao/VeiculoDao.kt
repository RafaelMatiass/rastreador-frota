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

    @Query("SELECT * FROM veiculos ORDER BY placa ASC")
    fun observarTodos(): Flow<List<VeiculoEntity>>

    @Query("SELECT * FROM veiculos WHERE id = :id")
    suspend fun buscarPorId(id: Long): VeiculoEntity?
}