package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.PreferenciasApp
import br.com.rastreadorfrota.geocoding.BuscaEnderecoService
import br.com.rastreadorfrota.geocoding.LugarEncontrado
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

data class EstadoBusca(
    val texto: String = "",
    val resultados: List<LugarEncontrado> = emptyList(),
    val buscando: Boolean = false,
    val erro: String? = null,
    val recentes: List<LugarEncontrado> = emptyList()
) {
    /** Já buscou e não veio nada (para mostrar "nenhum endereço encontrado"). */
    val semResultados: Boolean get() = texto.trim().length >= 3 && !buscando && erro == null && resultados.isEmpty()
}

/**
 * Autocomplete de endereço com debounce: só consulta a API 350 ms depois que a pessoa
 * para de digitar, e só a partir de 3 letras. Guarda as últimas buscas escolhidas.
 */
class BuscaEnderecoViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenciasApp(application)
    private val _estado = MutableStateFlow(EstadoBusca(recentes = lerRecentes()))
    val estado: StateFlow<EstadoBusca> = _estado.asStateFlow()

    private var busca: Job? = null

    fun digitar(texto: String, perto: GeoPoint = CENTRO_DISTRIBUICAO) {
        _estado.value = _estado.value.copy(texto = texto, erro = null)
        busca?.cancel()
        if (texto.trim().length < 3) {
            _estado.value = _estado.value.copy(resultados = emptyList(), buscando = false)
            return
        }
        busca = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _estado.value = _estado.value.copy(buscando = true)
            _estado.value = try {
                _estado.value.copy(resultados = BuscaEnderecoService.buscar(texto, perto), buscando = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.value.copy(resultados = emptyList(), buscando = false, erro = e.message ?: "Não foi possível buscar.")
            }
        }
    }

    fun limpar() {
        busca?.cancel()
        _estado.value = EstadoBusca(recentes = lerRecentes())
    }

    /** Guarda o lugar escolhido no topo das recentes. */
    fun lembrar(lugar: LugarEncontrado) {
        val linha = "${lugar.titulo}|${lugar.subtitulo}|${lugar.ponto.latitude}|${lugar.ponto.longitude}"
        prefs.buscasRecentes = listOf(linha) + prefs.buscasRecentes.filterNot { it.startsWith("${lugar.titulo}|") }
        _estado.value = _estado.value.copy(recentes = lerRecentes())
    }

    private fun lerRecentes(): List<LugarEncontrado> = prefs.buscasRecentes.mapNotNull { linha ->
        val partes = linha.split("|")
        if (partes.size < 4) return@mapNotNull null
        val lat = partes[2].toDoubleOrNull() ?: return@mapNotNull null
        val lon = partes[3].toDoubleOrNull() ?: return@mapNotNull null
        LugarEncontrado(partes[0], partes[1], GeoPoint(lat, lon))
    }

    private companion object {
        const val DEBOUNCE_MS = 350L
    }
}
