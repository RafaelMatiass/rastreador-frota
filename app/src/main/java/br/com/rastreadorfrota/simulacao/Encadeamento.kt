package br.com.rastreadorfrota.simulacao

import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import org.osmdroid.util.GeoPoint

/**
 * De onde e quando sai a próxima entrega de um veículo.
 * [fimDaAnterior]: novo fim da entrega anterior (a volta dela ao CD é cortada), ou null se não há anterior.
 */
data class Encadeamento(
    val inicio: Long,
    val origem: GeoPoint,
    val origemNome: String,
    val fimDaAnterior: Long?
)

/**
 * Fila de entregas do mesmo veículo: a nova entrega continua de onde a última da fila para.
 *
 * - Sem entrega ativa: sai do CD no próximo passo do relógio.
 * - A última ainda não terminou a parada no cliente: a nova sai DO CLIENTE, assim que a parada
 *   acaba, e a volta ao CD da anterior é cortada (só a última da fila volta ao CD).
 * - A última já está voltando (ou descarregando): a nova sai de onde o veículo estiver no
 *   próximo passo, e a anterior termina ali.
 *
 * Função pura (sem Android nem banco) para poder ser testada.
 */
fun encadearEntrega(ativas: List<EntregaEntity>, agora: Long): Encadeamento {
    val proximoPasso = (agora / SimuladorFrota.INTERVALO_MS + 1) * SimuladorFrota.INTERVALO_MS
    val ultima = ativas.maxByOrNull { it.fimPrevistoEm }
        ?: return Encadeamento(proximoPasso, CENTRO_DISTRIBUICAO, NOME_CD, null)

    val viagem = viagemDaEntrega(ultima)
    // Último passo parado no cliente: depois dele começaria a volta ao CD.
    val fimDaParada = viagem.inicioDaEtapa(EtapaViagem.VOLTA) ?: viagem.passos.size
    val fimDaParadaMs = ultima.inicioEm + fimDaParada * SimuladorFrota.INTERVALO_MS
    val inicio = maxOf(proximoPasso, fimDaParadaMs).coerceAtMost(ultima.fimPrevistoEm)
    // Onde o veículo está no último passo antes de [inicio].
    val indice = ((inicio - ultima.inicioEm) / SimuladorFrota.INTERVALO_MS - 1).toInt().coerceIn(0, viagem.passos.lastIndex)
    val t = viagem.passos[indice].telemetria
    val noCliente = inicio == fimDaParadaMs
    return Encadeamento(
        inicio = inicio,
        origem = if (noCliente) viagem.destino else GeoPoint(t.latitude, t.longitude),
        origemNome = if (noCliente) ultima.clienteNome else "posição de ${ultima.placa} a caminho do CD",
        fimDaAnterior = inicio
    )
}
