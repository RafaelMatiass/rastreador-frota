package br.com.rastreadorfrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.geocoding.Endereco
import br.com.rastreadorfrota.geocoding.GeocodingService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class EnderecosVeiculo(
    val veiculoId: Long? = null,
    /** Endereço da posição atual do veículo selecionado. */
    val atual: Endereco? = null,
    val buscando: Boolean = false,
    val erro: String? = null,
    /** Endereço completo do destino de entrega da viagem dele. */
    val entrega: Endereco? = null
)

/**
 * Endereço do veículo selecionado no mapa (Entrega 7).
 *
 * O veículo muda de posição a cada 3 s. Consultar a API a cada passo esgotaria a cota
 * diária em pouco tempo, então a posição só é consultada de novo quando ele andou
 * [DISTANCIA_MINIMA_M] E já se passaram [INTERVALO_MINIMO_MS] desde a última consulta.
 * Veículo parado não gera consulta nenhuma. Só o selecionado é consultado.
 */
class EnderecoViewModel : ViewModel() {

    private val _estado = MutableStateFlow(EnderecosVeiculo())
    val estado: StateFlow<EnderecosVeiculo> = _estado.asStateFlow()

    private var consultaAtual: Job? = null
    private var consultaEntrega: Job? = null
    private var ultimoPonto: GeoPoint? = null
    private var ultimoDestino: GeoPoint? = null
    private var ultimaConsultaMs = 0L

    /** Chamado a cada passo da simulação com o veículo selecionado (ou null). */
    fun acompanhar(veiculoId: Long?, posicao: GeoPoint?, destinoEntrega: GeoPoint?) {
        if (veiculoId == null || posicao == null) {
            limpar()
            return
        }
        if (veiculoId != _estado.value.veiculoId) {
            limpar()
            _estado.value = EnderecosVeiculo(veiculoId = veiculoId)
            consultarPosicao(posicao)
            ultimoDestino = destinoEntrega
            destinoEntrega?.let(::consultarEntrega)
            return
        }
        // O veículo entrou ou saiu de operação (motorista ativado/inativado): a entrega muda junto.
        if (destinoEntrega != ultimoDestino) {
            consultaEntrega?.cancel()
            ultimoDestino = destinoEntrega
            _estado.update { it.copy(entrega = null) }
            destinoEntrega?.let(::consultarEntrega)
        }
        val andou = ultimoPonto?.distanceToAsDouble(posicao) ?: Double.MAX_VALUE
        val esperou = System.currentTimeMillis() - ultimaConsultaMs
        if (consultaAtual?.isActive != true && andou >= DISTANCIA_MINIMA_M && esperou >= INTERVALO_MINIMO_MS) {
            consultarPosicao(posicao)
        }
    }

    private fun consultarPosicao(posicao: GeoPoint) {
        ultimoPonto = posicao
        ultimaConsultaMs = System.currentTimeMillis()
        _estado.update { it.copy(buscando = true) }
        consultaAtual = viewModelScope.launch {
            try {
                val endereco = GeocodingService.endereco(posicao)
                _estado.update { it.copy(atual = endereco, buscando = false, erro = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Mantém o último endereço conhecido, se houver, e mostra o motivo.
                _estado.update { it.copy(buscando = false, erro = e.message ?: "Endereço indisponível.") }
            }
        }
    }

    private fun consultarEntrega(destino: GeoPoint) {
        consultaEntrega = viewModelScope.launch {
            runCatching { GeocodingService.endereco(destino) }
                .onSuccess { endereco -> _estado.update { it.copy(entrega = endereco) } }
                .exceptionOrNull()?.let { if (it is CancellationException) throw it }
        }
    }

    private fun limpar() {
        consultaAtual?.cancel()
        consultaEntrega?.cancel()
        ultimoPonto = null
        ultimoDestino = null
        ultimaConsultaMs = 0L
        _estado.value = EnderecosVeiculo()
    }

    private companion object {
        const val DISTANCIA_MINIMA_M = 100.0
        const val INTERVALO_MINIMO_MS = 15_000L
    }
}
