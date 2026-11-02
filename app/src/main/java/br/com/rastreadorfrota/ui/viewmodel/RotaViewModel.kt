package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.geocoding.Endereco
import br.com.rastreadorfrota.geocoding.GeocodingService
import br.com.rastreadorfrota.rota.Rota
import br.com.rastreadorfrota.rota.RotaService
import br.com.rastreadorfrota.rota.localizacaoAtual
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Ponta da rota: a localização do aparelho ou um ponto fixo (endereço, veículo, CD, ponto do mapa). */
sealed interface PontoRota {
    val descricao: String

    data object MinhaLocalizacao : PontoRota {
        override val descricao = "Minha localização"
    }

    data class Fixo(
        val ponto: GeoPoint,
        override val descricao: String,
        /** Destino é um veículo da frota: o recalcular usa a posição nova dele. */
        val veiculoId: Long? = null
    ) : PontoRota
}

data class EstadoRota(
    val origemEscolhida: PontoRota = PontoRota.MinhaLocalizacao,
    val destinoEscolhido: PontoRota? = null,
    /** Coordenadas efetivas depois de resolver o GPS (ou cair no CD). */
    val origem: GeoPoint? = null,
    val destino: GeoPoint? = null,
    val rota: Rota? = null,
    val calculando: Boolean = false,
    /** Explica por que a origem não é o GPS (sem permissão, sem leitura, longe demais). */
    val aviso: String? = null,
    val erro: String? = null,
    /** Endereços da origem e do destino (reverse geocoding); null enquanto busca ou se falhar. */
    val enderecoOrigem: Endereco? = null,
    val enderecoDestino: Endereco? = null
) {
    val ativa: Boolean get() = destinoEscolhido != null
}

/**
 * Planejador de rotas (Entrega 6, ampliado): origem e destino escolhidos pelo usuário
 * (digitando, tocando no mapa, um veículo da frota ou o CD), rota por ruas no OSRM.
 * "Minha localização" usa o GPS; sem GPS utilizável, cai no centro de distribuição com aviso.
 */
class RotaViewModel(application: Application) : AndroidViewModel(application) {

    private val _estado = MutableStateFlow(EstadoRota())
    val estado: StateFlow<EstadoRota> = _estado.asStateFlow()

    private var calculo: Job? = null

    fun definirOrigem(origem: PontoRota) {
        _estado.update { it.copy(origemEscolhida = origem) }
        calcular()
    }

    fun definirDestino(destino: PontoRota) {
        _estado.update { it.copy(destinoEscolhido = destino) }
        calcular()
    }

    /** Atalho do mapa: "rota até aqui" saindo da localização atual. */
    fun tracarAte(destino: PontoRota.Fixo) {
        _estado.update { it.copy(origemEscolhida = PontoRota.MinhaLocalizacao, destinoEscolhido = destino) }
        calcular()
    }

    fun inverter() {
        val atual = _estado.value
        val destino = atual.destinoEscolhido ?: return
        _estado.update { it.copy(origemEscolhida = destino, destinoEscolhido = atual.origemEscolhida) }
        calcular()
    }

    /** [destinoAtualizado]: para destino-veículo, a posição de agora (ele andou). */
    fun recalcular(destinoAtualizado: PontoRota.Fixo? = null) {
        if (destinoAtualizado != null) _estado.update { it.copy(destinoEscolhido = destinoAtualizado) }
        calcular()
    }

    fun limpar() {
        calculo?.cancel()
        _estado.value = EstadoRota()
    }

    private fun calcular() {
        val pedido = _estado.value
        val destinoEscolhido = pedido.destinoEscolhido ?: return
        calculo?.cancel() // um pedido novo substitui o anterior
        _estado.value = EstadoRota(
            origemEscolhida = pedido.origemEscolhida,
            destinoEscolhido = destinoEscolhido,
            calculando = true
        )
        calculo = viewModelScope.launch {
            val origemResolvida = async { resolver(pedido.origemEscolhida, referencia = (destinoEscolhido as? PontoRota.Fixo)?.ponto) }
            val destinoResolvido = async { resolver(destinoEscolhido, referencia = (pedido.origemEscolhida as? PontoRota.Fixo)?.ponto) }
            val (origem, avisoOrigem) = origemResolvida.await()
            val (destino, avisoDestino) = destinoResolvido.await()
            _estado.update { it.copy(origem = origem, destino = destino, aviso = avisoOrigem ?: avisoDestino) }

            // Endereços em paralelo; se falharem, o card só não mostra o endereço.
            launch {
                runCatching { GeocodingService.endereco(origem) }
                    .onSuccess { e -> _estado.update { it.copy(enderecoOrigem = e) } }
            }
            launch {
                runCatching { GeocodingService.endereco(destino) }
                    .onSuccess { e -> _estado.update { it.copy(enderecoDestino = e) } }
            }
            try {
                val rota = RotaService.calcular(origem, destino, getApplication<Application>().packageName)
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

    /** Coordenada da ponta + aviso, se o GPS não pôde ser usado. */
    private suspend fun resolver(ponto: PontoRota, referencia: GeoPoint?): Pair<GeoPoint, String?> = when (ponto) {
        is PontoRota.Fixo -> ponto.ponto to null
        PontoRota.MinhaLocalizacao -> {
            val gps = localizacaoAtual(getApplication())
            val alvo = referencia ?: CENTRO_DISTRIBUICAO
            val distancia = gps?.distanceToAsDouble(alvo)
            when {
                gps != null && distancia != null && distancia <= DISTANCIA_MAXIMA_M -> gps to null
                gps == null -> CENTRO_DISTRIBUICAO to "Localização indisponível: usando o centro de distribuição como ponto de partida."
                // Caso típico do emulador, que fica na Califórnia por padrão.
                else -> CENTRO_DISTRIBUICAO to "Você está a ${(distancia!! / 1000).toInt()} km da região da frota: usando o centro de distribuição."
            }
        }
    }

    private companion object {
        /** Acima disso o GPS não é da região da frota (ex.: emulador sem localização definida). */
        const val DISTANCIA_MAXIMA_M = 300_000.0
    }
}
