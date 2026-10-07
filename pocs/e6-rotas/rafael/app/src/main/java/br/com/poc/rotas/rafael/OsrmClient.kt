package br.com.poc.rotas.rafael

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Rota por ruas: o desenho (lista de pontos), a distância e o tempo estimado. */
data class Rota(
    val pontos: List<GeoPoint>,
    val distanciaMetros: Double,
    val duracaoSegundos: Double
)

/**
 * Cliente mínimo do OSRM (Open Source Routing Machine), o serviço de rotas que usa
 * os dados do OpenStreetMap. Servidor público de demonstração, sem API key.
 *
 * GET /route/v1/driving/{lon},{lat};{lon},{lat}?overview=full&geometries=geojson
 *   - overview=full: devolve o desenho completo da rota, não uma versão simplificada;
 *   - geometries=geojson: coordenadas como [[lon, lat], ...] (atenção: LONGITUDE primeiro).
 */
object OsrmClient {

    private const val BASE = "https://router.project-osrm.org/route/v1/driving"

    suspend fun rota(origem: GeoPoint, destino: GeoPoint, userAgent: String): Rota =
        withContext(Dispatchers.IO) { // rede nunca na main thread
            val url = URL(
                "$BASE/${origem.longitude},${origem.latitude};${destino.longitude},${destino.latitude}" +
                    "?overview=full&geometries=geojson"
            )
            val conexao = url.openConnection() as HttpURLConnection
            conexao.connectTimeout = 10_000
            conexao.readTimeout = 15_000
            // A política de uso do servidor público pede um User-Agent que identifique o app.
            conexao.setRequestProperty("User-Agent", userAgent)
            try {
                val stream = if (conexao.responseCode in 200..299) conexao.inputStream else conexao.errorStream
                val corpo = stream?.bufferedReader()?.use { it.readText() }
                    ?: throw IOException("Resposta vazia do servidor de rotas (HTTP ${conexao.responseCode})")
                interpretar(JSONObject(corpo))
            } finally {
                conexao.disconnect()
            }
        }

    private fun interpretar(json: JSONObject): Rota {
        when (val codigo = json.optString("code")) {
            "Ok" -> Unit
            "NoRoute" -> throw IOException("Não existe rota por ruas entre os dois pontos.")
            else -> throw IOException("Servidor de rotas respondeu $codigo: ${json.optString("message")}")
        }
        val rota = json.getJSONArray("routes").getJSONObject(0)
        val coordenadas = rota.getJSONObject("geometry").getJSONArray("coordinates")
        val pontos = List(coordenadas.length()) { i ->
            val par = coordenadas.getJSONArray(i)
            GeoPoint(par.getDouble(1), par.getDouble(0)) // [lon, lat] -> GeoPoint(lat, lon)
        }
        return Rota(pontos, rota.getDouble("distance"), rota.getDouble("duration"))
    }
}
