package br.com.rastreadorfrota.simulacao

import br.com.rastreadorfrota.data.local.entity.EntregaEntity
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
    /** Cliente da entrega desta viagem (vazio sem entrega). */
    val clienteNome: String,
    val destinoNome: String,
    val destino: GeoPoint,
    /** Do ponto atual até o fim da etapa (vazio nas paradas): a linha que "encolhe" no mapa. */
    val trajetoRestante: List<GeoPoint>,
    val metrosRestantes: Double,
    /** Até o fim da etapa atual, em tempo de tela (passos × intervalo). */
    val segundosRestantes: Long,
    /** Entrega em andamento (null quando o veículo está no pátio). */
    val entregaId: Long? = null,
    /** Longe da rota planejada (mais de [LIMITE_DESVIO_M]): desvio de rota. */
    val foraDaRota: Boolean = false,
    /** Rota planejada da etapa atual, para o mapa mostrar por onde o veículo DEVERIA ir. */
    val rotaPlanejada: List<GeoPoint> = emptyList()
)

/**
 * Simula a posição e a telemetria da frota dentro do próprio app.
 *
 * O veículo só viaja quando o gestor atribui uma ENTREGA. A posição é função do relógio:
 * `passo da viagem = (agora - início da entrega) / INTERVALO`. Com isso:
 *  - sair e voltar pro mapa não reinicia a viagem (o veículo "continuou andando");
 *  - todos os aparelhos mostram o veículo no mesmo lugar sem trocar mensagens, porque a
 *    entrega (rotas e horário de início) é sincronizada e a conta é a mesma dos dois lados;
 *  - o histórico de uma entrega pode ser reconstruído a qualquer momento.
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

    /** Índice do passo da viagem no instante [passo] do relógio (pode ser negativo ou passar do fim). */
    fun indiceNaViagem(entrega: EntregaEntity, passo: Long): Long = passo - entrega.inicioEm / INTERVALO_MS

    /** Situação do veículo na entrega, ou null se ela ainda não começou ou já terminou. */
    fun situacao(entrega: EntregaEntity, passo: Long): SituacaoViagem? {
        val viagem = viagemDaEntrega(entrega)
        val indice = indiceNaViagem(entrega, passo)
        if (indice !in viagem.passos.indices) return null
        // Entrega encadeada: a volta ao CD foi cortada e o veículo já segue para a próxima.
        if (passo * INTERVALO_MS >= entrega.fimPrevistoEm) return null
        val i = indice.toInt()
        val atual = viagem.passos[i]
        val restante = atual.noTrajeto?.let { trajetoRestante(viagem.trajeto(atual.etapa), it) }.orEmpty()
        return SituacaoViagem(
            telemetria = atual.telemetria,
            etapa = atual.etapa,
            clienteNome = viagem.clienteNome,
            destinoNome = viagem.destinoNome,
            destino = viagem.destino,
            trajetoRestante = restante,
            metrosRestantes = comprimentoMetros(restante),
            segundosRestantes = (viagem.fimDaEtapa[i] - i + 1) * INTERVALO_MS / 1000,
            entregaId = entrega.id,
            foraDaRota = atual.foraDaRota,
            rotaPlanejada = viagem.planejado(atual.etapa)
        )
    }

    /** Tem motorista, mas nenhuma entrega em andamento: parado no pátio do CD, motor desligado. */
    fun aguardandoEntrega(placa: String): SituacaoViagem = noPatio(placa, EtapaViagem.AGUARDANDO_ENTREGA)

    /** Sem motorista ativo: parado no pátio do CD, motor desligado e portas fechadas. */
    fun foraDeOperacao(placa: String): SituacaoViagem = noPatio(placa, EtapaViagem.FORA_DE_OPERACAO)

    /** Cada placa ganha uma vaga fixa (~25 m do CD) pra os marcadores não ficarem sobrepostos. */
    private fun noPatio(placa: String, etapa: EtapaViagem): SituacaoViagem {
        val angulo = Math.toRadians((abs(placa.uppercase().hashCode()) % 360).toDouble())
        val vaga = GeoPoint(
            CENTRO_DISTRIBUICAO.latitude + 0.00022 * kotlin.math.cos(angulo),
            CENTRO_DISTRIBUICAO.longitude + 0.00022 * kotlin.math.sin(angulo)
        )
        return SituacaoViagem(
            telemetria = PontoTelemetria(vaga.latitude, vaga.longitude, 0, motorLigado = false, portasAbertas = false),
            etapa = etapa,
            clienteNome = "",
            destinoNome = "",
            destino = CENTRO_DISTRIBUICAO,
            trajetoRestante = emptyList(),
            metrosRestantes = 0.0,
            segundosRestantes = 0
        )
    }
}
