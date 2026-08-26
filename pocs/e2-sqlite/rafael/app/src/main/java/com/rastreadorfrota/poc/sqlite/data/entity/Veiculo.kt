package com.rastreadorfrota.poc.sqlite.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// motoristaId é opcional (nullable): um veículo pode existir sem motorista
// vinculado ainda, e se o motorista for removido o vínculo vira null em vez
// de apagar o veículo (SET_NULL).
@Entity(
    tableName = "veiculos",
    foreignKeys = [
        ForeignKey(
            entity = Motorista::class,
            parentColumns = ["id"],
            childColumns = ["motoristaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("motoristaId")]
)
data class Veiculo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placa: String,
    val modelo: String,
    val capacidadeCargaKg: Double,
    val motoristaId: Long? = null
)
