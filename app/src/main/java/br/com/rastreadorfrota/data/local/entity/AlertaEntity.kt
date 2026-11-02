package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoAlerta(val verbo: String) {
    ENTRADA("entrou em"),
    SAIDA("saiu de")
}

/** De onde veio o alerta: da posição simulada do veículo ou do GPS real do aparelho do motorista. */
enum class OrigemAlerta(val label: String) {
    SIMULADO("Telemetria simulada"),
    APARELHO("GPS do motorista")
}

/**
 * Uma entrada ou saída de cerca. O [id] é determinístico (cerca + placa + tipo + instante):
 * o mesmo evento visto de novo não duplica, nem no Room nem no Firestore.
 * Só os alertas do APARELHO sobem para a nuvem; os simulados qualquer aparelho do gestor recalcula.
 */
@Entity(tableName = "alertas")
data class AlertaEntity(
    @PrimaryKey
    val id: String,
    val cercaNome: String,
    val placa: String,
    val tipo: String,
    val origem: String,
    val instante: Long,
    val motoristaNome: String? = null,
    val uid: String? = null,
    val lido: Boolean = false,
    val sincronizado: Boolean = true
) {
    val tipoEnum: TipoAlerta get() = TipoAlerta.entries.find { it.name == tipo } ?: TipoAlerta.ENTRADA
    val origemEnum: OrigemAlerta get() = OrigemAlerta.entries.find { it.name == origem } ?: OrigemAlerta.SIMULADO

    /** "ABC1D23 entrou em Supermercado Central" */
    val descricao: String get() = "$placa ${tipoEnum.verbo} $cercaNome"
}
