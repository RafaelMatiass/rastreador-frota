package br.com.rastreadorfrota.geofence

import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Onde um veículo está agora, para conferir as cercas. */
data class PosicaoVeiculo(val placa: String, val tipo: String, val latitude: Double, val longitude: Double)

/** Um veículo cruzou a borda de uma cerca. */
data class Transicao(val cerca: CercaEntity, val placa: String, val tipo: TipoAlerta)

/**
 * Geofencing da frota simulada, como função pura (sem Android): dado onde cada veículo estava
 * (dentro/fora de cada cerca) e onde está agora, devolve o novo estado e as transições.
 *
 * - A 1ª avaliação ([anterior] null) só registra o estado: abrir o app não gera uma rajada de alertas.
 * - Par veículo/cerca que ainda não existia (cerca nova, veículo novo) também só é registrado.
 * - A transição só vira alerta se a cerca pede aviso daquele tipo (entrada e/ou saída).
 */
object AvaliadorCercas {

    fun avaliar(
        anterior: Map<String, Boolean>?,
        posicoes: List<PosicaoVeiculo>,
        cercas: List<CercaEntity>
    ): Pair<Map<String, Boolean>, List<Transicao>> {
        val estado = mutableMapOf<String, Boolean>()
        val transicoes = mutableListOf<Transicao>()
        for (cerca in cercas) {
            if (!cerca.ativa) continue
            for (p in posicoes) {
                if (!cerca.seAplicaA(p.placa, p.tipo)) continue
                val chave = chave(cerca, p.placa)
                val dentro = distanciaMetros(cerca.latitude, cerca.longitude, p.latitude, p.longitude) <= cerca.raioM
                estado[chave] = dentro
                val antes = anterior?.get(chave) ?: continue
                if (antes == dentro) continue
                val tipo = if (dentro) TipoAlerta.ENTRADA else TipoAlerta.SAIDA
                val avisar = if (dentro) cerca.notificarEntrada else cerca.notificarSaida
                if (avisar) transicoes += Transicao(cerca, p.placa, tipo)
            }
        }
        return estado to transicoes
    }

    private fun chave(cerca: CercaEntity, placa: String) = "${cerca.id}|$placa"

    /** Distância em metros entre duas coordenadas (fórmula de haversine). */
    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }
}
