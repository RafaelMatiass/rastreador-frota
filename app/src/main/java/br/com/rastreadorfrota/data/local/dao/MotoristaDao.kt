package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoristaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(motorista: MotoristaEntity): Long

    @Update
    suspend fun atualizar(motorista: MotoristaEntity)

    @Delete
    suspend fun deletar(motorista: MotoristaEntity)

    @Query("SELECT * FROM motoristas ORDER BY nome ASC")
    fun observarTodos(): Flow<List<MotoristaEntity>>

    @Query("SELECT * FROM motoristas WHERE id = :id")
    suspend fun buscarPorId(id: Long): MotoristaEntity?
}