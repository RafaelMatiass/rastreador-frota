package br.com.poc.rotas.rafael

import org.osmdroid.util.GeoPoint

/** Um ponto sobre o trajeto e o índice do segmento (trajeto[segmento] -> trajeto[segmento + 1]) em que ele caiu. */
data class PontoNoTrajeto(val ponto: GeoPoint, val segmento: Int)

/**
 * Anda ao longo do trajeto e marca um ponto a cada [passoMetros], sempre SOBRE a linha.
 * É isso que faz o veículo simulado seguir as ruas, em vez de cortar caminho em linha reta.
 */
fun amostrar(trajeto: List<GeoPoint>, passoMetros: Double): List<PontoNoTrajeto> {
    if (trajeto.size < 2) return trajeto.map { PontoNoTrajeto(it, 0) }
    val saida = mutableListOf(PontoNoTrajeto(trajeto.first(), 0))
    var faltaParaProximo = passoMetros
    for (i in 0 until trajeto.size - 1) {
        val a = trajeto[i]
        val b = trajeto[i + 1]
        val comprimento = a.distanceToAsDouble(b)
        var percorrido = 0.0
        while (comprimento - percorrido >= faltaParaProximo) {
            percorrido += faltaParaProximo
            val f = percorrido / comprimento
            saida += PontoNoTrajeto(
                GeoPoint(a.latitude + (b.latitude - a.latitude) * f, a.longitude + (b.longitude - a.longitude) * f),
                i
            )
            faltaParaProximo = passoMetros
        }
        faltaParaProximo -= comprimento - percorrido
    }
    // Garante a chegada exatamente no destino.
    if (saida.last().ponto.distanceToAsDouble(trajeto.last()) > 1.0) {
        saida += PontoNoTrajeto(trajeto.last(), trajeto.size - 2)
    }
    return saida
}

/** O que falta percorrer: o ponto atual e os vértices seguintes (a linha "encolhe" conforme o veículo anda). */
fun trajetoRestante(trajeto: List<GeoPoint>, atual: PontoNoTrajeto): List<GeoPoint> =
    listOf(atual.ponto) + trajeto.subList(atual.segmento + 1, trajeto.size)

fun comprimentoMetros(pontos: List<GeoPoint>): Double =
    pontos.zipWithNext { a, b -> a.distanceToAsDouble(b) }.sum()
