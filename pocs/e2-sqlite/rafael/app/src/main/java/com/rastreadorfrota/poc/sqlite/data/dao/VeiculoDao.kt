package com.rastreadorfrota.poc.sqlite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rastreadorfrota.poc.sqlite.data.entity.Veiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VeiculoDao {
    @Insert
    suspend fun inserir(veiculo: Veiculo): Long

    @Query("SELECT * FROM veiculos ORDER BY id DESC")
    fun listarTodos(): Flow<List<Veiculo>>
}
