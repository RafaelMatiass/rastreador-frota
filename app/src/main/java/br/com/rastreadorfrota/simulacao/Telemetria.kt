package br.com.rastreadorfrota.simulacao

enum class StatusVeiculo(val label: String) {
    EM_TRANSITO("Em trânsito"),
    PARADO("Parado"),
    /** Fora de operação: nenhum motorista ativo associado ao veículo. */
    SEM_MOTORISTA("Sem motorista")
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
