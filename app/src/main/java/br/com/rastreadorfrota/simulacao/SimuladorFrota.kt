package br.com.rastreadorfrota.simulacao

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

/** Onde o veículo está na viagem dele, para o mapa mostrar destino, trajeto e tempo restante. */
data class SituacaoViagem(
    val telemetria: PontoTelemetria,
    val etapa: EtapaViagem,
    val destinoNome: String,
    val destino: GeoPoint,
    /** Do ponto atual até o fim da etapa (vazio nas paradas): a linha que "encolhe" no mapa. */
    val trajetoRestante: List<GeoPoint>,
    val metrosRestantes: Double,
    /** Até o fim da etapa atual, em tempo de tela (passos × intervalo). */
    val segundosRestantes: Long
)

/**
 * Simula a posição e a telemetria da frota dentro do próprio app.
 *
 * O passo da simulação é derivado do RELÓGIO, não de um contador em memória:
 * `passo = agora / INTERVALO`. Com isso:
 *  - sair e voltar pro mapa não reinicia a viagem (o veículo "continuou andando");
 *  - dois aparelhos (controlador e motorista) mostram o mesmo veículo no mesmo lugar,
 *    sem trocar nenhuma mensagem, porque a conta é a mesma dos dois lados.
 *
 * Cada veículo recebe uma viagem e um deslocamento fixos a partir da placa
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

    fun situacao(placa: String, passo: Long): SituacaoViagem {
        val semente = abs(placa.uppercase().hashCode().toLong())
        val viagem = viagensSimuladas[(semente % viagensSimuladas.size).toInt()]
        val total = viagem.passos.size
        val deslocamento = (semente / viagensSimuladas.size) % total
        val indice = ((passo + deslocamento) % total).toInt()
        val atual = viagem.passos[indice]
        val restante = atual.noTrajeto?.let { trajetoRestante(viagem.trajeto(atual.etapa), it) }.orEmpty()
        return SituacaoViagem(
            telemetria = atual.telemetria,
            etapa = atual.etapa,
            destinoNome = viagem.destinoNome,
            destino = viagem.destino,
            trajetoRestante = restante,
            metrosRestantes = comprimentoMetros(restante),
            segundosRestantes = (viagem.fimDaEtapa[indice] - indice + 1) * INTERVALO_MS / 1000
        )
    }

    fun telemetria(placa: String, passo: Long): PontoTelemetria = situacao(placa, passo).telemetria
}
