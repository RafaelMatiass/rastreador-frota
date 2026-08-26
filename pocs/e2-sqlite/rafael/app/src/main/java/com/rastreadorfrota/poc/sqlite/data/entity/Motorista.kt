package com.rastreadorfrota.poc.sqlite.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "motoristas")
data class Motorista(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val cnh: String,
    val telefone: String
)
