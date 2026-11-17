package br.com.rastreadorfrota.simulacao

import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import org.osmdroid.util.GeoPoint
import java.util.concurrent.ConcurrentHashMap

/** Centro de distribuição (CD) da Distribuidora Morada do Sol em Araraquara: início e fim de todas as viagens. */
const val NOME_CD = "CD Morada do Sol"
val CENTRO_DISTRIBUICAO = GeoPoint(-21.815086, -48.204879)

/** Acima desta distância da rota planejada o veículo está "fora da rota". */
const val LIMITE_DESVIO_M = 60.0

enum class EtapaViagem(val label: String) {
    IDA("Indo para a entrega"),
    ENTREGA("Entregando"),
    VOLTA("Voltando ao CD"),
    DESCARGA("Descarregando no CD"),
    /** Tem motorista, mas o gestor ainda não atribuiu uma entrega: espera no pátio do CD. */
    AGUARDANDO_ENTREGA("Aguardando entrega"),
    /** Sem motorista ativo associado: o veículo não viaja, fica no pátio do CD. */
    FORA_DE_OPERACAO("Sem motorista ativo")
}

/**
 * Um passo da viagem: a telemetria, onde ele cai no trajeto da etapa (null nas paradas) e
 * se, neste ponto, o veículo está longe da rota PLANEJADA (detecção de desvio).
 */
internal class PassoViagem(
    val telemetria: PontoTelemetria,
    val etapa: EtapaViagem,
    val noTrajeto: PontoNoTrajeto?,
    val foraDaRota: Boolean = false
)

/**
 * Viagem de uma entrega: CD -> cliente -> CD, pelas ruas.
 * [passos] é a sequência que a simulação percorre, um passo a cada [SimuladorFrota.INTERVALO_MS]
 * a partir do início da entrega; [fimDaEtapa] guarda, para cada passo, o índice do último passo
 * da mesma etapa (para calcular o tempo restante).
 */
class Viagem internal constructor(
    /** Cliente que recebe a entrega (ex.: "Supermercado Central"). */
    val clienteNome: String,
    val destinoNome: String,
    /** Ida como o gestor planejou; [ida] é a que o veículo percorre (com desvio, se houver). */
    internal val idaPlanejada: List<GeoPoint>,
    internal val ida: List<GeoPoint>,
    internal val volta: List<GeoPoint>,
    internal val passos: List<PassoViagem>
) {
    val destino: GeoPoint get() = idaPlanejada.last()
    val duracaoMs: Long get() = passos.size * SimuladorFrota.INTERVALO_MS

    internal val fimDaEtapa: IntArray = IntArray(passos.size).also { fim ->
        for (i in passos.indices.reversed()) {
            fim[i] = if (i < passos.lastIndex && passos[i + 1].etapa == passos[i].etapa) fim[i + 1] else i
        }
    }

    /** Trajeto que o veículo percorre na etapa. */
    internal fun trajeto(etapa: EtapaViagem): List<GeoPoint> = when (etapa) {
        EtapaViagem.IDA -> ida
        EtapaViagem.VOLTA -> volta
        else -> emptyList()
    }

    /** Trajeto planejado da etapa: a referência para medir o desvio. */
    fun planejado(etapa: EtapaViagem): List<GeoPoint> = when (etapa) {
        EtapaViagem.IDA -> idaPlanejada
        EtapaViagem.VOLTA -> volta
        else -> emptyList()
    }

    /** Por onde o veículo passou até o passo [ate] (inclusive), na ordem: o histórico do trajeto. */
    fun percorrido(ate: Int): List<GeoPoint> =
        passos.take((ate + 1).coerceIn(0, passos.size)).map { GeoPoint(it.telemetria.latitude, it.telemetria.longitude) }

    /** Trechos feitos fora da rota até o passo [ate]: cada um é uma linha para pintar no histórico. */
    fun trechosForaDaRota(ate: Int): List<List<GeoPoint>> {
        val trechos = mutableListOf<List<GeoPoint>>()
        var atual = mutableListOf<GeoPoint>()
        passos.take((ate + 1).coerceIn(0, passos.size)).forEach { p ->
            if (p.foraDaRota) atual += GeoPoint(p.telemetria.latitude, p.telemetria.longitude)
            else if (atual.isNotEmpty()) { trechos += atual; atual = mutableListOf() }
        }
        if (atual.isNotEmpty()) trechos += atual
        return trechos
    }

    /** Índice do primeiro passo de uma etapa (ex.: chegada ao cliente), ou null. */
    fun inicioDaEtapa(etapa: EtapaViagem): Int? = passos.indexOfFirst { it.etapa == etapa }.takeIf { it >= 0 }
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

    /**
     * Percorre [trajeto] pelas ruas a [kmh], com [semaforos] paradas rápidas espalhadas pelo caminho.
     * Cada ponto é comparado com [planejado]: longe demais, o passo fica marcado como fora da rota.
     */
    fun percurso(trajeto: List<GeoPoint>, planejado: List<GeoPoint>, etapa: EtapaViagem, kmh: Int, semaforos: Int) = apply {
        val metrosPorPasso = kmh / 3.6 * (SimuladorFrota.INTERVALO_MS / 1000.0) * ACELERACAO
        val amostra = amostrar(trajeto, metrosPorPasso)
        val ondeParar = (1..semaforos).map { it * amostra.size / (semaforos + 1) }.toSet()
        val mesmaRota = trajeto === planejado
        amostra.forEachIndexed { i, p ->
            val fora = !mesmaRota && distanciaAoTrajeto(p.ponto, planejado) > LIMITE_DESVIO_M
            if (i in ondeParar) {
                repeat(PASSOS_SEMAFORO) { passos += PassoViagem(ponto(p.ponto, 0, motorLigado = true, portasAbertas = false), etapa, p, fora) }
            }
            passos += PassoViagem(ponto(p.ponto, kmh, motorLigado = true, portasAbertas = false), etapa, p, fora)
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

/**
 * Sequência da entrega: ida (com semáforos) -> entrega (motor desligado, portas abertas)
 * -> volta -> descarga no CD. Função pura: os testes e a criação da entrega usam a mesma conta.
 */
fun montarViagem(
    clienteNome: String,
    destinoNome: String,
    idaPlanejada: List<GeoPoint>,
    idaExecutada: List<GeoPoint>,
    volta: List<GeoPoint>,
    kmhIda: Int,
    kmhVolta: Int
): Viagem {
    // Mesma lista = sem desvio: dispensa medir a distância a cada passo.
    val ida = if (idaExecutada == idaPlanejada) idaPlanejada else idaExecutada
    val passos = MontadorViagem()
        .percurso(ida, idaPlanejada, EtapaViagem.IDA, kmhIda, semaforos = 2)
        .parada(EtapaViagem.ENTREGA, PASSOS_ENTREGA, motorLigado = false, portasAbertas = true)
        .percurso(volta, volta, EtapaViagem.VOLTA, kmhVolta, semaforos = 1)
        .parada(EtapaViagem.DESCARGA, PASSOS_DESCARGA, motorLigado = false, portasAbertas = true)
        .passos
    return Viagem(clienteNome, destinoNome, idaPlanejada, ida, volta, passos)
}

private val cacheViagens = ConcurrentHashMap<String, Viagem>()

/** Viagem de uma entrega, montada uma vez (decodificar e amostrar a cada 3 s seria desperdício). */
fun viagemDaEntrega(e: EntregaEntity): Viagem {
    val chave = "${e.remoteId ?: "l${e.id}"}|${e.rotaIdaExecutada.hashCode()}|${e.rotaVolta.hashCode()}"
    return cacheViagens.getOrPut(chave) {
        val planejada = PolylineCodec.decodificar(e.rotaIda)
        val executada = if (e.rotaIdaExecutada == e.rotaIda) planejada else PolylineCodec.decodificar(e.rotaIdaExecutada)
        montarViagem(e.clienteNome, e.endereco, planejada, executada, PolylineCodec.decodificar(e.rotaVolta), e.kmhIda, e.kmhVolta)
    }
}
