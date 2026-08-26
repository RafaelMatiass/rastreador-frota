package com.rastreadorfrota.poc.sqlite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "negociantes")
data class Negociante(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cnpjCpf: String,
    val telefone: String,
    val endereco: String
)
