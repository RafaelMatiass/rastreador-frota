package br.com.rastreadorfrota.simulacao

import org.osmdroid.util.GeoPoint

/**
 * Decodifica uma polyline no formato do Google/OSRM (precisão 5): cada coordenada é a
 * diferença para a anterior, multiplicada por 1e5 e escrita em blocos de 5 bits como caracteres ASCII.
 * https://developers.google.com/maps/documentation/utilities/polylinealgorithm
 */
object PolylineCodec {

    fun decodificar(codificada: String): List<GeoPoint> {
        val pontos = mutableListOf<GeoPoint>()
        var indice = 0
        var lat = 0
        var lon = 0
        fun proximoValor(): Int {
            var resultado = 0
            var deslocamento = 0
            var bloco: Int
            do {
                bloco = codificada[indice++].code - 63
                resultado = resultado or ((bloco and 0x1f) shl deslocamento)
                deslocamento += 5
            } while (bloco >= 0x20)
            return if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1
        }
        while (indice < codificada.length) {
            lat += proximoValor()
            lon += proximoValor()
            pontos += GeoPoint(lat / 1e5, lon / 1e5)
        }
        return pontos
    }

    /** Caminho inverso: guarda a rota de uma entrega como texto compacto (Room e Firestore). */
    fun codificar(pontos: List<GeoPoint>): String {
        val saida = StringBuilder()
        var latAnterior = 0
        var lonAnterior = 0
        fun escrever(valor: Int) {
            var v = if (valor < 0) (valor shl 1).inv() else valor shl 1
            while (v >= 0x20) {
                saida.append(((0x20 or (v and 0x1f)) + 63).toChar())
                v = v shr 5
            }
            saida.append((v + 63).toChar())
        }
        for (p in pontos) {
            val lat = Math.round(p.latitude * 1e5).toInt()
            val lon = Math.round(p.longitude * 1e5).toInt()
            escrever(lat - latAnterior)
            escrever(lon - lonAnterior)
            latAnterior = lat
            lonAnterior = lon
        }
        return saida.toString()
    }
}
