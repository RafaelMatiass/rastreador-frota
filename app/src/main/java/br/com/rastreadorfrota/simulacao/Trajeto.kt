package br.com.rastreadorfrota.simulacao

import org.osmdroid.util.GeoPoint

/** Um ponto sobre o trajeto e o índice do segmento (trajeto[segmento] -> trajeto[segmento + 1]) em que ele caiu. */
data class PontoNoTrajeto(val ponto: GeoPoint, val segmento: Int)

/**
 * Anda ao longo do trajeto e marca um ponto a cada [passoMetros], sempre SOBRE a linha.
 * É isso que faz o veículo simulado seguir as ruas em vez de cortar caminho em linha reta.
 * Validado antes na PoC `pocs/e6-rotas/rafael`.
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
    // Garante a chegada exatamente no fim do trajeto.
    if (saida.last().ponto.distanceToAsDouble(trajeto.last()) > 1.0) {
        saida += PontoNoTrajeto(trajeto.last(), trajeto.size - 2)
    }
    return saida
}

/** O que falta percorrer: o ponto atual e os vértices seguintes (a linha "encolhe" conforme o veículo anda). */
fun trajetoRestante(trajeto: List<GeoPoint>, atual: PontoNoTrajeto): List<GeoPoint> =
    listOf(atual.ponto) + trajeto.subList(atual.segmento + 1, trajeto.size)

/**
 * Menor distância, em metros, entre um ponto e a linha de um trajeto: a base da detecção
 * de desvio. Projeta tudo num plano local em metros (equiretangular em torno do ponto),
 * preciso o bastante para poucos quilômetros, e mede a distância a cada segmento.
 */
fun distanciaAoTrajeto(p: GeoPoint, trajeto: List<GeoPoint>): Double {
    if (trajeto.isEmpty()) return Double.MAX_VALUE
    if (trajeto.size == 1) return p.distanceToAsDouble(trajeto[0])
    val mLat = 110_540.0
    val mLon = 111_320.0 * kotlin.math.cos(Math.toRadians(p.latitude))
    var menor = Double.MAX_VALUE
    for (i in 0 until trajeto.size - 1) {
        val ax = (trajeto[i].longitude - p.longitude) * mLon
        val ay = (trajeto[i].latitude - p.latitude) * mLat
        val dx = (trajeto[i + 1].longitude - p.longitude) * mLon - ax
        val dy = (trajeto[i + 1].latitude - p.latitude) * mLat - ay
        val comp2 = dx * dx + dy * dy
        // Projeção do ponto (origem do plano) no segmento, limitada às pontas.
        val t = if (comp2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / comp2).coerceIn(0.0, 1.0)
        val d = kotlin.math.hypot(ax + t * dx, ay + t * dy)
        if (d < menor) menor = d
    }
    return menor
}

/**
 * Ponto fora da rota, usado para simular um desvio: anda [fracao] do trajeto e se afasta
 * [afastamentoM] metros para o lado (perpendicular à direção da rua naquele trecho).
 */
fun pontoDeDesvio(trajeto: List<GeoPoint>, fracao: Double, afastamentoM: Double): GeoPoint {
    var alvo = comprimentoMetros(trajeto) * fracao
    for (i in 0 until trajeto.size - 1) {
        val a = trajeto[i]
        val b = trajeto[i + 1]
        val seg = a.distanceToAsDouble(b)
        if (alvo <= seg && seg > 0) {
            val f = alvo / seg
            val lat = a.latitude + (b.latitude - a.latitude) * f
            val lon = a.longitude + (b.longitude - a.longitude) * f
            val mLat = 110_540.0
            val mLon = 111_320.0 * kotlin.math.cos(Math.toRadians(lat))
            val dx = (b.longitude - a.longitude) * mLon
            val dy = (b.latitude - a.latitude) * mLat
            val norma = kotlin.math.hypot(dx, dy)
            // Perpendicular (-dy, dx) em metros, convertida de volta para graus.
            return GeoPoint(lat + (dx / norma) * afastamentoM / mLat, lon + (-dy / norma) * afastamentoM / mLon)
        }
        alvo -= seg
    }
    return trajeto.last()
}

fun comprimentoMetros(pontos: List<GeoPoint>): Double =
    pontos.zipWithNext { a, b -> a.distanceToAsDouble(b) }.sum()
