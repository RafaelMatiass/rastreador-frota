package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.rota.Rota
import br.com.rastreadorfrota.rota.RotaService
import br.com.rastreadorfrota.rota.localizacaoAtual
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Para onde a rota vai: um veículo da frota ([veiculoId]) ou um ponto escolhido no mapa (destino de entrega). */
data class DestinoRota(val ponto: GeoPoint, val descricao: String, val veiculoId: Long? = null)

enum class OrigemRota(val label: String) {
    GPS("Sua localização"),
    CENTRO_DISTRIBUICAO("Centro de distribuição")
}

data class EstadoRota(
    val destino: DestinoRota? = null,
    val origem: GeoPoint? = null,
    val tipoOrigem: OrigemRota? = null,
    val rota: Rota? = null,
    val calculando: Boolean = false,
    /** Explica por que a origem não é o GPS (sem permissão, sem leitura, longe demais). */
    val aviso: String? = null,
    val erro: String? = null
)

/**
 * Rota da localização atual até um ponto do mapa (Entrega 6).
 * Origem: GPS do aparelho; sem GPS utilizável, o centro de distribuição.
 */
class RotaViewModel(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoRota())
    val estado: StateFlow<EstadoRota> = _estado.asStateFlow()

    private var calculo: Job? = null

    fun tracar(destino: DestinoRota) {
        calculo?.cancel() // um pedido novo substitui o anterior
        _estado.value = EstadoRota(destino = destino, calculando = true)
        calculo = viewModelScope.launch {
            val gps = localizacaoAtual(getApplication())
            val distanciaGps = gps?.distanceToAsDouble(destino.ponto)
            val usarGps = distanciaGps != null && distanciaGps <= DISTANCIA_MAXIMA_M
            val origem = if (usarGps) gps!! else CENTRO_DISTRIBUICAO
            _estado.update {
                it.copy(
                    origem = origem,
                    tipoOrigem = if (usarGps) OrigemRota.GPS else OrigemRota.CENTRO_DISTRIBUICAO,
                    aviso = when {
                        usarGps -> null
                        distanciaGps == null -> "Localização indisponível: a rota parte do centro de distribuição."
                        // Caso típico do emulador, que fica na Califórnia por padrão.
                        else -> "Você está a ${(distanciaGps / 1000).toInt()} km do destino: " +
                            "a rota parte do centro de distribuição."
                    }
                )
            }
            try {
                val rota = RotaService.calcular(origem, destino.ponto, getApplication<Application>().packageName)
                _estado.update { it.copy(rota = rota, calculando = false) }
            } catch (e: CancellationException) {
                throw e // pedido substituído: não mexe no estado do pedido novo
            } catch (e: Exception) {
                val mensagem = when (e) {
                    is UnknownHostException, is SocketTimeoutException ->
                        "Sem conexão com o servidor de rotas. Verifique a internet."
                    else -> e.message ?: "Não foi possível calcular a rota."
                }
                _estado.update { it.copy(calculando = false, erro = mensagem) }
            }
        }
    }

    fun limpar() {
        calculo?.cancel()
        _estado.value = EstadoRota()
    }

    private companion object {
        /** Acima disso o GPS não é da região da frota (ex.: emulador sem localização definida). */
        const val DISTANCIA_MAXIMA_M = 300_000.0
    }
}
