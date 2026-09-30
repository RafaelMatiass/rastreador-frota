package br.com.poc.mapa.rafael

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Um instante da viagem: posição + a telemetria daquele ponto. */
data class PontoTelemetria(
    val lat: Double,
    val lon: Double,
    val velocidadeKmh: Int,
    val motorLigado: Boolean,
    val portasAbertas: Boolean
) {
    val emTransito: Boolean get() = velocidadeKmh > 0
}

/**
 * Monta uma sequência PRÉ-DETERMINADA de pontos a partir de trechos e paradas.
 * Nada aqui é aleatório: rodar duas vezes gera exatamente a mesma viagem.
 */
class Rota {
    private val pontos = mutableListOf<PontoTelemetria>()
    private var atual: Pair<Double, Double>? = null

    fun inicio(lat: Double, lon: Double) = apply { atual = lat to lon }

    /** Anda em linha reta até (lat, lon) em [passos] pontos, a [kmh]. */
    fun trecho(lat: Double, lon: Double, passos: Int, kmh: Int) = apply {
        val (lat0, lon0) = atual!!
        for (i in 1..passos) {
            val f = i.toDouble() / passos
            pontos += PontoTelemetria(lat0 + (lat - lat0) * f, lon0 + (lon - lon0) * f, kmh, true, false)
        }
        atual = lat to lon
    }

    /** Fica parado [passos] pontos (semáforo: motor ligado; entrega: motor desligado e portas abertas). */
    fun parada(passos: Int, motorLigado: Boolean, portasAbertas: Boolean) = apply {
        val (lat, lon) = atual!!
        repeat(passos) { pontos += PontoTelemetria(lat, lon, 0, motorLigado, portasAbertas) }
    }

    fun pontos(): List<PontoTelemetria> = pontos.toList()
}

// Duas viagens em Araraquara/SP.
val rotaCentro = Rota().inicio(-21.7946, -48.1756)
    .trecho(-21.8009, -48.1682, 4, 42)
    .parada(2, motorLigado = true, portasAbertas = false)     // semáforo
    .trecho(-21.8072, -48.1604, 4, 36)
    .parada(3, motorLigado = false, portasAbertas = true)     // entrega
    .trecho(-21.7946, -48.1756, 6, 50)
    .pontos()

val rotaVilaXavier = Rota().inicio(-21.7780, -48.1850)
    .trecho(-21.7700, -48.1760, 5, 38)
    .parada(3, motorLigado = false, portasAbertas = true)     // entrega
    .trecho(-21.7760, -48.1650, 4, 45)
    .parada(1, motorLigado = true, portasAbertas = false)     // semáforo
    .trecho(-21.7780, -48.1850, 6, 40)
    .pontos()

data class VeiculoSimulado(val placa: String, val rota: List<PontoTelemetria>, val passo: Int) {
    val ponto: PontoTelemetria get() = rota[passo % rota.size]
}

/**
 * UM loop de coroutine avança todos os veículos juntos. A tela só observa
 * o [StateFlow]; ela não sabe nada de tempo nem de rota.
 */
class SimuladorViewModel : ViewModel() {
    private val _veiculos = MutableStateFlow(
        listOf(
            VeiculoSimulado("ABC1D23", rotaCentro, 0),
            VeiculoSimulado("XYZ4E56", rotaVilaXavier, 0)
        )
    )
    val veiculos: StateFlow<List<VeiculoSimulado>> = _veiculos.asStateFlow()

    private val _pausado = MutableStateFlow(false)
    val pausado: StateFlow<Boolean> = _pausado.asStateFlow()

    private val _tick = MutableStateFlow(0)
    val tick: StateFlow<Int> = _tick.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(INTERVALO_MS)
                if (!_pausado.value) {
                    _tick.update { it + 1 }
                    _veiculos.update { lista -> lista.map { it.copy(passo = it.passo + 1) } }
                }
            }
        }
    }

    fun alternarPausa() = _pausado.update { !it }

    companion object {
        const val INTERVALO_MS = 2_000L
    }
}
