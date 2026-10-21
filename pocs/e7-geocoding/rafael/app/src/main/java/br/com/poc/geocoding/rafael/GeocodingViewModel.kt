package br.com.poc.geocoding.rafael

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.util.Locale

data class EstadoGeocoding(
    val ponto: GeoPoint? = null,
    val endereco: Endereco? = null,
    val buscando: Boolean = false,
    val erro: String? = null,
    val veioDoCache: Boolean = false,
    val tempoMs: Long = 0,
    val chamadasApi: Int = 0,
    val acertosCache: Int = 0
)

/**
 * Toque no mapa -> coordenada -> endereço.
 *
 * Cache em memória por coordenada ARREDONDADA (4 casas ≈ 11 m): tocar de novo
 * no mesmo lugar não gasta outra requisição. No app isso importa, porque os
 * veículos mudam de posição a cada 3 s e a API tem cota diária.
 */
class GeocodingViewModel : ViewModel() {

    private val _estado = MutableStateFlow(EstadoGeocoding())
    val estado: StateFlow<EstadoGeocoding> = _estado.asStateFlow()

    private val cache = mutableMapOf<String, Endereco>()
    private var busca: Job? = null

    fun buscar(ponto: GeoPoint) {
        busca?.cancel() // um toque novo substitui a busca anterior
        val chave = chaveCache(ponto)
        cache[chave]?.let { salvo ->
            _estado.update {
                it.copy(ponto = ponto, endereco = salvo, buscando = false, erro = null,
                    veioDoCache = true, tempoMs = 0, acertosCache = it.acertosCache + 1)
            }
            return
        }
        _estado.update { it.copy(ponto = ponto, endereco = null, buscando = true, erro = null) }
        busca = viewModelScope.launch {
            val inicio = System.currentTimeMillis()
            try {
                val endereco = GeoapifyClient.endereco(ponto.latitude, ponto.longitude, BuildConfig.GEOAPIFY_API_KEY)
                cache[chave] = endereco
                _estado.update {
                    it.copy(endereco = endereco, buscando = false, veioDoCache = false,
                        tempoMs = System.currentTimeMillis() - inicio, chamadasApi = it.chamadasApi + 1)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(buscando = false, erro = e.message ?: "Falha ao buscar o endereço.") }
            }
        }
    }

    private fun chaveCache(p: GeoPoint) = String.format(Locale.US, "%.4f,%.4f", p.latitude, p.longitude)
}
