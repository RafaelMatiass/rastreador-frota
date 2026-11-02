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
    val fotoLocalPath: String? = null,
    val ativo: Boolean = true,
    val sincronizado: Boolean = false,        // false = tem alteração local pendente de envio
    val remoteId: String? = null,             // id do documento no Firestore (nulo até subir pela 1ª vez)
    val updatedAt: Long = System.currentTimeMillis(), // usado no last-write-wins do pull
    val deletedLocally: Boolean = false,      // soft delete: só apaga de vez após confirmar no Firestore
    // v5: fluxo "motorista propõe, gestor aprova" e vistoria do motorista
    val situacao: String = SituacaoVeiculo.ATIVO.name,
    val cadastradoPorUid: String? = null,
    val odometroKm: Int? = null,
    val ultimaVistoriaEm: Long? = null,
    val observacoes: String? = null
) {
    val situacaoEnum: SituacaoVeiculo get() = SituacaoVeiculo.de(situacao)

    /** Só veículo aprovado entra na operação (atribuição, mapa, simulação). */
    val operacional: Boolean get() = situacaoEnum == SituacaoVeiculo.ATIVO
}
