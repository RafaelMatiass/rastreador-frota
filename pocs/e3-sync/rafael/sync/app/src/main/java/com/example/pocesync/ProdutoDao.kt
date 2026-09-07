package com.example.pocesync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProdutoDao {

    @Insert
    suspend fun inserir(produto: ProdutoEntity)

    @Query("SELECT * FROM produtos ORDER BY nome")
    suspend fun buscarTodos(): List<ProdutoEntity>

    @Query("SELECT * FROM produtos WHERE sincronizado = 0")
    suspend fun buscarNaoSincronizados(): List<ProdutoEntity>

    @Query("UPDATE produtos SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarComoSincronizado(id: String)
}
