package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "veiculos")
data class VeiculoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placa: String,
    val modelo: String,
    val tipo: String, // nome do TipoVeiculo
    val capacidadeCargaKg: Double? = null,
    val ativo: Boolean = true,
    // preparado para a Entrega 3 (sincronização com Firestore)
    val sincronizado: Boolean = false
)