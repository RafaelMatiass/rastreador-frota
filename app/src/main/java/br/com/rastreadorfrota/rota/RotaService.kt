package br.com.rastreadorfrota.rota

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
) {
    val distanciaTexto: String
        get() = if (distanciaMetros < 1000) "${distanciaMetros.toInt()} m"
        else "%.1f km".format(distanciaMetros / 1000)

    val duracaoTexto: String
        get() {
            val minutos = (duracaoSegundos / 60).toInt().coerceAtLeast(1)
            return if (minutos < 60) "$minutos min" else "${minutos / 60} h ${minutos % 60} min"
        }
}

/**
 * Geração de rotas pelo OSRM (Open Source Routing Machine), que usa os dados do
 * OpenStreetMap: mesmo ecossistema do mapa (osmdroid) e sem API key.
 * Validado antes na PoC `pocs/e6-rotas/rafael`.
 *
 * GET /route/v1/driving/{lon},{lat};{lon},{lat}?overview=full&geometries=geojson
 * As coordenadas da resposta vêm como [LONGITUDE, latitude].
 *
 * A rota não vai para o Room nem para o Firestore: é calculada na hora, a partir de
 * posições que mudam (o próprio aparelho e o veículo simulado).
 */
object RotaService {

    private const val BASE = "https://router.project-osrm.org/route/v1/driving"

    suspend fun calcular(origem: GeoPoint, destino: GeoPoint, userAgent: String): Rota =
        withContext(Dispatchers.IO) {
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
                    ?: throw IOException("Resposta vazia do servidor de rotas (HTTP ${conexao.responseCode}).")
                interpretar(JSONObject(corpo))
            } finally {
                conexao.disconnect()
            }
        }

    private fun interpretar(json: JSONObject): Rota {
        when (val codigo = json.optString("code")) {
            "Ok" -> Unit
            "NoRoute" -> throw IOException("Não existe rota por ruas entre os dois pontos.")
            else -> throw IOException("O servidor de rotas respondeu $codigo: ${json.optString("message")}")
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
