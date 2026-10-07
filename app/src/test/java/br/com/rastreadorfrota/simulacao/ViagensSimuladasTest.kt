package br.com.rastreadorfrota.simulacao

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViagensSimuladasTest {

    @Test
    fun `decodifica o exemplo da documentacao do Google`() {
        val pontos = PolylineCodec.decodificar("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, pontos.size)
        assertEquals(38.5, pontos[0].latitude, 1e-5)
        assertEquals(-120.2, pontos[0].longitude, 1e-5)
        assertEquals(43.252, pontos[2].latitude, 1e-5)
        assertEquals(-126.453, pontos[2].longitude, 1e-5)
    }

    @Test
    fun `viagens saem do CD e voltam ao CD pelas ruas`() {
        viagensSimuladas.forEach { v ->
            assertTrue(v.ida.first().distanceToAsDouble(CENTRO_DISTRIBUICAO) < 50)
            assertTrue(v.volta.last().distanceToAsDouble(CENTRO_DISTRIBUICAO) < 50)
            assertTrue("trajeto por ruas tem muitos vértices", v.ida.size > 50)
        }
    }

    @Test
    fun `passos consecutivos ficam proximos e seguem a ordem das etapas`() {
        val ordem = listOf(EtapaViagem.IDA, EtapaViagem.ENTREGA, EtapaViagem.VOLTA, EtapaViagem.DESCARGA)
        viagensSimuladas.forEach { v ->
            val pontos = v.passos.map { it.telemetria }
            pontos.zipWithNext().forEach { (a, b) ->
                val metros = org.osmdroid.util.GeoPoint(a.latitude, a.longitude)
                    .distanceToAsDouble(org.osmdroid.util.GeoPoint(b.latitude, b.longitude))
                assertTrue("salto de $metros m", metros <= 250)
            }
            assertEquals(ordem, v.passos.map { it.etapa }.distinct())
        }
    }

    @Test
    fun `situacao encolhe o trajeto restante conforme o veiculo anda`() {
        val placa = "ABC1D23"
        val inicio = (0L..2000L).first { SimuladorFrota.situacao(placa, it).etapa == EtapaViagem.IDA }
        val agora = SimuladorFrota.situacao(placa, inicio)
        val depois = SimuladorFrota.situacao(placa, inicio + 5)
        if (depois.etapa == EtapaViagem.IDA) {
            assertTrue(depois.metrosRestantes < agora.metrosRestantes)
            assertTrue(depois.segundosRestantes < agora.segundosRestantes)
        }
        val entregando = (0L..2000L).map { SimuladorFrota.situacao(placa, it) }.first { it.etapa == EtapaViagem.ENTREGA }
        assertTrue(entregando.trajetoRestante.isEmpty())
        assertEquals(0, entregando.telemetria.velocidadeKmh)
        assertTrue(entregando.telemetria.portasAbertas)
    }
}
