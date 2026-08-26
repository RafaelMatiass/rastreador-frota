package com.rastreadorfrota.poc.sqlite.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.rastreadorfrota.poc.sqlite.data.entity.Produto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    @Insert
    suspend fun inserir(produto: Produto): Long

    @Query("SELECT * FROM produtos ORDER BY id DESC")
    fun listarTodos(): Flow<List<Produto>>
}
