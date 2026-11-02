package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A quem a cerca se aplica: todos os veículos, um tipo (grupo) ou uma lista de placas. */
enum class AlvoCerca(val label: String) {
    TODOS("Toda a frota"),
    TIPO("Um tipo de veículo"),
    VEICULOS("Veículos específicos");

    companion object {
        fun de(nome: String?): AlvoCerca = entries.find { it.name == nome } ?: TODOS
    }
}

/**
 * Perímetro circular (geofence) criado pelo gestor (Entrega 8).
 * Segue o mesmo padrão de sincronização dos veículos: grava no Room e sobe para
 * "cercas" no Firestore de forma assíncrona.
 *
 * [alvoValor]: nome do TipoVeiculo (alvo TIPO) ou placas separadas por vírgula (alvo VEICULOS).
 * Placa, e não id, porque é a mesma em todos os aparelhos.
 */
@Entity(tableName = "cercas")
data class CercaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val latitude: Double,
    val longitude: Double,
    val raioM: Double,
    val alvo: String = AlvoCerca.TODOS.name,
    val alvoValor: String = "",
    val notificarEntrada: Boolean = true,
    val notificarSaida: Boolean = true,
    val ativa: Boolean = true,
    val sincronizado: Boolean = false,
    val remoteId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedLocally: Boolean = false
) {
    val alvoEnum: AlvoCerca get() = AlvoCerca.de(alvo)

    val placasAlvo: Set<String>
        get() = alvoValor.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    /** A cerca vale para este veículo? */
    fun seAplicaA(placa: String, tipo: String): Boolean = when (alvoEnum) {
        AlvoCerca.TODOS -> true
        AlvoCerca.TIPO -> alvoValor == tipo
        AlvoCerca.VEICULOS -> placa.uppercase() in placasAlvo
    }
}
