package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "motoristas",
    foreignKeys = [
        ForeignKey(
            entity = VeiculoEntity::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("veiculoId")]
)
data class MotoristaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val cnh: String,
    val telefone: String,
    val email: String,
    val veiculoId: Long? = null,
    val ativo: Boolean = true,
    val sincronizado: Boolean = false,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedLocally: Boolean = false
)