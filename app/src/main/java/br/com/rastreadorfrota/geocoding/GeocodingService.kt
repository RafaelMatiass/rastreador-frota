package br.com.rastreadorfrota.geocoding

import br.com.rastreadorfrota.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Endereço devolvido pela API, já separado em partes. */
data class Endereco(
    val linha1: String,           // "Avenida Barroso, 1200"
    val linha2: String,           // "Centro, Araraquara - SP, 14800-000, Brasil"
    val bairro: String?,
    val cidade: String?,
    val cep: String?
) {
    /** "Avenida Barroso, 1200 – Centro": cabe numa linha do painel. */
    val resumo: String get() = listOfNotNull(linha1, bairro ?: cidade).joinToString(" – ")
}

/**
 * Reverse geocoding (Entrega 7): coordenada -> endereço, pela Reverse Geocoding API da
 * Geoapify, citada no manual da disciplina (dados do OpenStreetMap; 3.000 requisições/dia grátis).
 * Validado antes na PoC `pocs/e7-geocoding/rafael`.
 *
 * GET https://api.geoapify.com/v1/geocode/reverse?lat=..&lon=..&lang=pt&format=json&apiKey=..
 *
 * A chave vem de `local.properties` (GEOAPIFY_API_KEY), que fica fora do Git.
 * Cache em memória por coordenada arredondada (4 casas ≈ 11 m): o mesmo lugar não
 * gasta outra requisição (veículo parado, destinos fixos das viagens, toque repetido).
 */
object GeocodingService {

    private const val BASE = "https://api.geoapify.com/v1/geocode/reverse"
    private val cache = ConcurrentHashMap<String, Endereco>()

    suspend fun endereco(ponto: GeoPoint): Endereco {
        val chave = String.format(Locale.US, "%.4f,%.4f", ponto.latitude, ponto.longitude)
        cache[chave]?.let { return it }
        return consultar(ponto.latitude, ponto.longitude).also { cache[chave] = it }
    }

    private suspend fun consultar(lat: Double, lon: Double): Endereco = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) throw IOException("Chave da Geoapify ausente (GEOAPIFY_API_KEY em local.properties).")
        val conexao = URL("$BASE?lat=$lat&lon=$lon&lang=pt&format=json&apiKey=$apiKey")
            .openConnection() as HttpURLConnection
        conexao.connectTimeout = 10_000
        conexao.readTimeout = 15_000
        try {
            val codigo = conexao.responseCode
            when (codigo) {
                in 200..299 -> interpretar(JSONObject(conexao.inputStream.bufferedReader().use { it.readText() }))
                401 -> throw IOException("Chave da Geoapify inválida.")
                429 -> throw IOException("Limite diário de consultas de endereço atingido.")
                else -> throw IOException("Serviço de endereços respondeu HTTP $codigo.")
            }
        } catch (e: UnknownHostException) {
            throw IOException("Sem conexão para buscar o endereço.", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("Sem conexão para buscar o endereço.", e)
        } finally {
            conexao.disconnect()
        }
    }

    private fun interpretar(json: JSONObject): Endereco {
        val resultados = json.optJSONArray("results")
        if (resultados == null || resultados.length() == 0) throw IOException("Nenhum endereço encontrado aqui.")
        val r = resultados.getJSONObject(0)
        fun campo(nome: String) = r.optString(nome).takeIf { it.isNotBlank() }
        return Endereco(
            linha1 = campo("address_line1") ?: campo("formatted") ?: "Endereço sem nome",
            linha2 = campo("address_line2").orEmpty(),
            bairro = campo("suburb") ?: campo("district"),
            cidade = campo("city"),
            cep = campo("postcode")
        )
    }
}
