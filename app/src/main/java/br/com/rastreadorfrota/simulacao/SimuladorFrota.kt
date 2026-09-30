package br.com.rastreadorfrota.simulacao

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlin.math.abs

/**
 * Simula a posição e a telemetria da frota dentro do próprio app.
 *
 * O passo da simulação é derivado do RELÓGIO, não de um contador em memória:
 * `passo = agora / INTERVALO`. Com isso:
 *  - sair e voltar pro mapa não reinicia a viagem (o veículo "continuou andando");
 *  - dois aparelhos (controlador e motorista) mostram o mesmo veículo no mesmo lugar,
 *    sem trocar nenhuma mensagem, porque a conta é a mesma dos dois lados.
 *
 * Cada veículo recebe uma rota e um deslocamento fixos a partir da placa
 * (String.hashCode é estável entre execuções/aparelhos), pra não ficarem sobrepostos.
 *
 * A telemetria fica só em memória: o manual pede a simulação "no próprio aplicativo",
 * e gravar um ponto a cada poucos segundos no Firestore gastaria cota à toa.
 */
object SimuladorFrota {

    const val INTERVALO_MS = 3_000L

    /** Emite o passo atual e depois um novo a cada [INTERVALO_MS], alinhado ao relógio. */
    val passos: Flow<Long> = flow {
        while (true) {
            val agora = System.currentTimeMillis()
            emit(agora / INTERVALO_MS)
            delay(INTERVALO_MS - agora % INTERVALO_MS)
        }
    }.distinctUntilChanged()

    fun telemetria(placa: String, passo: Long): PontoTelemetria {
        val semente = abs(placa.uppercase().hashCode().toLong())
        val rota = rotasSimuladas[(semente % rotasSimuladas.size).toInt()]
        val deslocamento = (semente / rotasSimuladas.size) % rota.size
        return rota[((passo + deslocamento) % rota.size).toInt()]
    }
}
