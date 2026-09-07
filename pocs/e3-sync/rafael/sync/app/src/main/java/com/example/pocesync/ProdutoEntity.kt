package com.example.pocesync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class ProdutoEntity(
    @PrimaryKey
    val id: String,
    val nome: String,
    val preco: Double,
    val sincronizado: Boolean = false
)
