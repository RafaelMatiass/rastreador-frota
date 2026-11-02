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
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Um resultado da busca de endereço: o que mostrar na lista e onde fica. */
data class LugarEncontrado(
    val titulo: String,     // "Avenida Barroso, 1200"
    val subtitulo: String,  // "Centro, Araraquara - SP"
    val ponto: GeoPoint
)

/**
 * Busca de endereço digitado (forward geocoding / autocomplete) para o planejador de rotas,
 * pela Autocomplete API da Geoapify: mesma chave e mesmos dados (OpenStreetMap) do reverse geocoding.
 *
 * GET https://api.geoapify.com/v1/geocode/autocomplete?text=..&lang=pt&filter=countrycode:br
 *     &bias=proximity:lon,lat&limit=6&format=json&apiKey=..
 *
 * Os resultados mais próximos de [perto] (o CD, por padrão) vêm primeiro. Cache por texto
 * para não gastar cota quando a pessoa apaga e digita de novo.
 */
object BuscaEnderecoService {

    private const val BASE = "https://api.geoapify.com/v1/geocode/autocomplete"
    private val cache = ConcurrentHashMap<String, List<LugarEncontrado>>()

    suspend fun buscar(texto: String, perto: GeoPoint): List<LugarEncontrado> {
        val consulta = texto.trim()
        if (consulta.length < 3) return emptyList()
        val chave = consulta.lowercase(Locale.ROOT)
        cache[chave]?.let { return it }
        return consultar(consulta, perto).also { cache[chave] = it }
    }

    private suspend fun consultar(texto: String, perto: GeoPoint): List<LugarEncontrado> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY
        if (apiKey.isBlank()) throw IOException("Chave da Geoapify ausente (GEOAPIFY_API_KEY em local.properties).")
        val url = URL(
            "$BASE?text=${URLEncoder.encode(texto, "UTF-8")}&lang=pt&limit=6&filter=countrycode:br" +
                "&bias=proximity:${perto.longitude},${perto.latitude}&format=json&apiKey=$apiKey"
        )
        val conexao = url.openConnection() as HttpURLConnection
        conexao.connectTimeout = 8_000
        conexao.readTimeout = 10_000
        try {
            when (val codigo = conexao.responseCode) {
                in 200..299 -> interpretar(JSONObject(conexao.inputStream.bufferedReader().use { it.readText() }))
                401 -> throw IOException("Chave da Geoapify inválida.")
                429 -> throw IOException("Limite diário de buscas atingido.")
                else -> throw IOException("Busca de endereços respondeu HTTP $codigo.")
            }
        } catch (e: UnknownHostException) {
            throw IOException("Sem conexão para buscar endereços.", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("Sem conexão para buscar endereços.", e)
        } finally {
            conexao.disconnect()
        }
    }

    private fun interpretar(json: JSONObject): List<LugarEncontrado> {
        val resultados = json.optJSONArray("results") ?: return emptyList()
        return List(resultados.length()) { i ->
            val r = resultados.getJSONObject(i)
            fun campo(nome: String) = r.optString(nome).takeIf { it.isNotBlank() }
            LugarEncontrado(
                titulo = campo("address_line1") ?: campo("name") ?: campo("formatted") ?: "Local sem nome",
                subtitulo = campo("address_line2").orEmpty(),
                ponto = GeoPoint(r.getDouble("lat"), r.getDouble("lon"))
            )
        }.distinctBy { it.titulo + it.subtitulo }
    }
}
