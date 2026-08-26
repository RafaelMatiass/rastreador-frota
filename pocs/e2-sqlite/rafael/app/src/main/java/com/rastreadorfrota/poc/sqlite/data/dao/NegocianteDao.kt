package com.rastreadorfrota.poc.sqlite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rastreadorfrota.poc.sqlite.data.entity.Negociante
import kotlinx.coroutines.flow.Flow

@Dao
interface NegocianteDao {
    @Insert
    suspend fun inserir(negociante: Negociante): Long

    @Query("SELECT * FROM negociantes ORDER BY id DESC")
    fun listarTodos(): Flow<List<Negociante>>
}