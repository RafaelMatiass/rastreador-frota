package br.com.rastreadorfrota.simulacao

enum class StatusVeiculo(val label: String) {
    EM_TRANSITO("Em trânsito"),
    PARADO("Parado")
}

/** Um instante da viagem simulada: posição e a telemetria daquele ponto. */
data class PontoTelemetria(
    val latitude: Double,
    val longitude: Double,
    val velocidadeKmh: Int,
    val motorLigado: Boolean,
    val portasAbertas: Boolean
) {
    val status: StatusVeiculo
        get() = if (velocidadeKmh > 0) StatusVeiculo.EM_TRANSITO else StatusVeiculo.PARADO
}
