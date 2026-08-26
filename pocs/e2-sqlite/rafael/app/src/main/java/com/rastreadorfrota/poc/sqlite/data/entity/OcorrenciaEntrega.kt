package com.rastreadorfrota.poc.sqlite.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Registra o status de uma entrega de um produto: em trânsito, entregue,
// atrasada ou avariada. Vincula opcionalmente o veículo que estava
// transportando no momento do registro — é isso que conecta o dado ao
// rastreamento de fato (posição/estado do veículo na rota).
@Entity(
    tableName = "ocorrencias_entrega",
    foreignKeys = [
        ForeignKey(
            entity = Produto::class,
            parentColumns = ["id"],
            childColumns = ["produtoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Veiculo::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("produtoId"), Index("veiculoId")]
)
data class OcorrenciaEntrega(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val produtoId: Long,
    val veiculoId: Long? = null,
    val status: String, // EM_TRANSITO, ENTREGUE, ATRASADO, AVARIADO
    val observacao: String,
    val dataOcorrencia: Long = System.currentTimeMillis()
)
