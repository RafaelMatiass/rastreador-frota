package br.com.rastreadorfrota.simulacao

import org.osmdroid.util.GeoPoint

/** Centro de distribuição (CD) da frota em Araraquara: início e fim de todas as viagens. */
val CENTRO_DISTRIBUICAO = GeoPoint(-21.815086, -48.204879)

enum class EtapaViagem(val label: String) {
    IDA("Indo para a entrega"),
    ENTREGA("Entregando"),
    VOLTA("Voltando ao CD"),
    DESCARGA("Descarregando no CD")
}

/** Um passo da viagem: a telemetria e onde ele cai no trajeto da etapa (null nas paradas). */
internal class PassoViagem(
    val telemetria: PontoTelemetria,
    val etapa: EtapaViagem,
    val noTrajeto: PontoNoTrajeto?
)

/**
 * Viagem pré-determinada: CD -> destino de entrega -> CD, sempre pelas ruas.
 * [passos] é a sequência fixa que a simulação percorre em loop; [fimDaEtapa] guarda,
 * para cada passo, o índice do último passo da mesma etapa (para calcular o tempo restante).
 */
class Viagem internal constructor(
    val destinoNome: String,
    internal val ida: List<GeoPoint>,
    internal val volta: List<GeoPoint>,
    internal val passos: List<PassoViagem>
) {
    val destino: GeoPoint get() = ida.last()

    internal val fimDaEtapa: IntArray = IntArray(passos.size).also { fim ->
        for (i in passos.indices.reversed()) {
            fim[i] = if (i < passos.lastIndex && passos[i + 1].etapa == passos[i].etapa) fim[i + 1] else i
        }
    }

    internal fun trajeto(etapa: EtapaViagem): List<GeoPoint> = when (etapa) {
        EtapaViagem.IDA -> ida
        EtapaViagem.VOLTA -> volta
        else -> emptyList()
    }
}

/**
 * Simulação acelerada: a cada passo (3 s na tela) o veículo percorre o que andaria em
 * 3 s × [ACELERACAO]. Assim uma ida leva poucos minutos na demonstração, e a telemetria
 * continua mostrando a velocidade "real".
 */
internal const val ACELERACAO = 3
private const val PASSOS_SEMAFORO = 2
private const val PASSOS_ENTREGA = 20 // 1 min parado no cliente
private const val PASSOS_DESCARGA = 10

/** Monta a sequência de passos: andando (motor ligado, portas fechadas) ou parado com a telemetria da parada. */
private class MontadorViagem {
    val passos = mutableListOf<PassoViagem>()
    private var ultimo: GeoPoint? = null

    /** Percorre o trajeto pelas ruas a [kmh], com [semaforos] paradas rápidas espalhadas pelo caminho. */
    fun percurso(trajeto: List<GeoPoint>, etapa: EtapaViagem, kmh: Int, semaforos: Int) = apply {
        val metrosPorPasso = kmh / 3.6 * (SimuladorFrota.INTERVALO_MS / 1000.0) * ACELERACAO
        val amostra = amostrar(trajeto, metrosPorPasso)
        val ondeParar = (1..semaforos).map { it * amostra.size / (semaforos + 1) }.toSet()
        amostra.forEachIndexed { i, p ->
            if (i in ondeParar) {
                repeat(PASSOS_SEMAFORO) { passos += PassoViagem(ponto(p.ponto, 0, motorLigado = true, portasAbertas = false), etapa, p) }
            }
            passos += PassoViagem(ponto(p.ponto, kmh, motorLigado = true, portasAbertas = false), etapa, p)
        }
        ultimo = trajeto.last()
    }

    fun parada(etapa: EtapaViagem, quantidade: Int, motorLigado: Boolean, portasAbertas: Boolean) = apply {
        val aqui = ultimo!!
        repeat(quantidade) { passos += PassoViagem(ponto(aqui, 0, motorLigado, portasAbertas), etapa, null) }
    }

    private fun ponto(p: GeoPoint, kmh: Int, motorLigado: Boolean, portasAbertas: Boolean) =
        PontoTelemetria(p.latitude, p.longitude, kmh, motorLigado, portasAbertas)
}

/** [kmhIda] e [kmhVolta]: velocidade média que o OSRM estimou para cada trajeto. */
private fun viagem(destinoNome: String, ida: String, kmhIda: Int, volta: String, kmhVolta: Int): Viagem {
    val trajetoIda = PolylineCodec.decodificar(ida)
    val trajetoVolta = PolylineCodec.decodificar(volta)
    val passos = MontadorViagem()
        .percurso(trajetoIda, EtapaViagem.IDA, kmhIda, semaforos = 2)
        .parada(EtapaViagem.ENTREGA, PASSOS_ENTREGA, motorLigado = false, portasAbertas = true)
        .percurso(trajetoVolta, EtapaViagem.VOLTA, kmhVolta, semaforos = 1)
        .parada(EtapaViagem.DESCARGA, PASSOS_DESCARGA, motorLigado = false, portasAbertas = true)
        .passos
    return Viagem(destinoNome, trajetoIda, trajetoVolta, passos)
}

/** As 4 viagens da frota (trajetos em `TrajetosRuas.kt`). Nada é aleatório: geram sempre os mesmos passos. */
internal val viagensSimuladas: List<Viagem> = listOf(
    viagem("Rua Papa João Paulo I", TRAJETO_IDA_1, 56, TRAJETO_VOLTA_1, 48),
    viagem("Avenida Queiroz Filho", TRAJETO_IDA_2, 47, TRAJETO_VOLTA_2, 38),
    viagem("Avenida Barroso", TRAJETO_IDA_3, 37, TRAJETO_VOLTA_3, 36),
    viagem("Acesso Rodoviário Abdo Najn", TRAJETO_IDA_4, 65, TRAJETO_VOLTA_4, 37)
)
