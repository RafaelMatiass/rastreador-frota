package com.rastreadorfrota.poc.sqlite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rastreadorfrota.poc.sqlite.data.entity.OcorrenciaEntrega
import kotlinx.coroutines.flow.Flow

@Dao
interface OcorrenciaEntregaDao {
    @Insert
    suspend fun inserir(ocorrencia: OcorrenciaEntrega): Long

    @Query("SELECT * FROM ocorrencias_entrega ORDER BY id DESC")
    fun listarTodas(): Flow<List<OcorrenciaEntrega>>
}
