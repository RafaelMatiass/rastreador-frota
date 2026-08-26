package com.rastreadorfrota.poc.sqlite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rastreadorfrota.poc.sqlite.data.entity.Motorista
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoristaDao {
    @Insert
    suspend fun inserir(motorista: Motorista): Long

    @Query("SELECT * FROM motoristas ORDER BY id DESC")
    fun listarTodos(): Flow<List<Motorista>>
}
