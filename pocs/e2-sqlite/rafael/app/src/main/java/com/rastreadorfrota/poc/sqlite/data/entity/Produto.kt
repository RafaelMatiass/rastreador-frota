package com.rastreadorfrota.poc.sqlite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produtos")
data class Produto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String,
    val pesoKg: Double,
    val quantidade: Int
)
