package br.com.poc.geocoding.rafael

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Endereço devolvido pela API, já separado em partes. */
data class Endereco(
    val linha1: String,           // "Avenida Barroso, 1200"
    val linha2: String,           // "Centro, Araraquara - SP, 14800-000, Brasil"
    val completo: String,
    val rua: String?,
    val numero: String?,
    val bairro: String?,
    val cidade: String?,
    val estado: String?,
    val cep: String?,
    val distanciaMetros: Double   // distância entre o ponto pedido e o endereço encontrado
)

/**
 * Cliente mínimo da Reverse Geocoding API da Geoapify (citada no manual da disciplina),
 * que usa os dados do OpenStreetMap. Conta gratuita: 3.000 requisições por dia.
 *
 * GET https://api.geoapify.com/v1/geocode/reverse?lat=..&lon=..&lang=pt&format=json&apiKey=..
 */
object GeoapifyClient {

    private const val BASE = "https://api.geoapify.com/v1/geocode/reverse"

    suspend fun endereco(lat: Double, lon: Double, chave: String): Endereco = withContext(Dispatchers.IO) {
        if (chave.isBlank()) {
            throw IOException("Chave da Geoapify ausente: coloque GEOAPIFY_API_KEY=... em local.properties.")
        }
        val conexao = URL("$BASE?lat=$lat&lon=$lon&lang=pt&format=json&apiKey=$chave")
            .openConnection() as HttpURLConnection
        conexao.connectTimeout = 10_000
        conexao.readTimeout = 15_000
        try {
            val codigo = conexao.responseCode
            val corpo = (if (codigo in 200..299) conexao.inputStream else conexao.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            when (codigo) {
                in 200..299 -> interpretar(JSONObject(corpo))
                401 -> throw IOException("Chave da Geoapify inválida (HTTP 401).")
                429 -> throw IOException("Limite de requisições da Geoapify atingido (HTTP 429).")
                else -> throw IOException("Geoapify respondeu HTTP $codigo: ${corpo.take(200)}")
            }
        } finally {
            conexao.disconnect()
        }
    }

    private fun interpretar(json: JSONObject): Endereco {
        val resultados = json.optJSONArray("results")
        if (resultados == null || resultados.length() == 0) {
            throw IOException("Nenhum endereço encontrado para este ponto.")
        }
        val r = resultados.getJSONObject(0)
        fun campo(nome: String) = r.optString(nome).takeIf { it.isNotBlank() }
        return Endereco(
            linha1 = campo("address_line1") ?: "Endereço sem nome",
            linha2 = campo("address_line2").orEmpty(),
            completo = campo("formatted").orEmpty(),
            rua = campo("street"),
            numero = campo("housenumber"),
            bairro = campo("suburb") ?: campo("district"),
            cidade = campo("city"),
            estado = campo("state_code") ?: campo("state"),
            cep = campo("postcode"),
            distanciaMetros = r.optDouble("distance", 0.0)
        )
    }
}
