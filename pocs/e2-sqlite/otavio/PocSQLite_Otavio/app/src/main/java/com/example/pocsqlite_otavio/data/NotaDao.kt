package com.example.pocsqlite_otavio.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {
    @Insert
    suspend fun inserir(nota: NotaEntity)

    @Query("SELECT * FROM notas ORDER BY id DESC")
    fun observarTodas(): Flow<List<NotaEntity>>
}