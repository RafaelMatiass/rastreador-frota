package br.com.rastreadorfrota.geofence

import br.com.rastreadorfrota.data.local.entity.AlvoCerca
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AvaliadorCercasTest {

    // Cerca de 200 m em volta de um ponto; "dentro" fica a ~0 m e "fora" a ~1,1 km.
    private val cerca = CercaEntity(id = 1, nome = "CD", latitude = -21.8150, longitude = -48.2048, raioM = 200.0)
    private fun dentro(placa: String = "ABC1D23", tipo: String = "VAN") = PosicaoVeiculo(placa, tipo, -21.8150, -48.2048)
    private fun fora(placa: String = "ABC1D23", tipo: String = "VAN") = PosicaoVeiculo(placa, tipo, -21.8250, -48.2048)

    @Test
    fun `primeira avaliacao so registra o estado, sem alertas`() {
        val (estado, transicoes) = AvaliadorCercas.avaliar(null, listOf(dentro()), listOf(cerca))
        assertTrue(transicoes.isEmpty())
        assertEquals(true, estado.values.single())
    }

    @Test
    fun `sair e entrar geram SAIDA e ENTRADA`() {
        val (e1, _) = AvaliadorCercas.avaliar(null, listOf(dentro()), listOf(cerca))
        val (e2, saida) = AvaliadorCercas.avaliar(e1, listOf(fora()), listOf(cerca))
        assertEquals(listOf(TipoAlerta.SAIDA), saida.map { it.tipo })
        val (_, entrada) = AvaliadorCercas.avaliar(e2, listOf(dentro()), listOf(cerca))
        assertEquals(listOf(TipoAlerta.ENTRADA), entrada.map { it.tipo })
    }

    @Test
    fun `cerca so de entrada nao avisa a saida, e cerca pausada nao avisa nada`() {
        val soEntrada = cerca.copy(notificarSaida = false)
        val (e1, _) = AvaliadorCercas.avaliar(null, listOf(dentro()), listOf(soEntrada))
        assertTrue(AvaliadorCercas.avaliar(e1, listOf(fora()), listOf(soEntrada)).second.isEmpty())

        val pausada = cerca.copy(ativa = false)
        val (p1, _) = AvaliadorCercas.avaliar(null, listOf(dentro()), listOf(pausada))
        assertTrue(AvaliadorCercas.avaliar(p1, listOf(fora()), listOf(pausada)).second.isEmpty())
    }

    @Test
    fun `cerca por tipo ou por placa ignora os outros veiculos`() {
        val soCaminhao = cerca.copy(alvo = AlvoCerca.TIPO.name, alvoValor = "CAMINHAO")
        val (e1, _) = AvaliadorCercas.avaliar(null, listOf(dentro(tipo = "VAN"), dentro("XYZ9876", "CAMINHAO")), listOf(soCaminhao))
        val (_, t) = AvaliadorCercas.avaliar(e1, listOf(fora(tipo = "VAN"), fora("XYZ9876", "CAMINHAO")), listOf(soCaminhao))
        assertEquals(listOf("XYZ9876"), t.map { it.placa })

        val soUma = cerca.copy(alvo = AlvoCerca.VEICULOS.name, alvoValor = "abc1d23")
        val (s1, _) = AvaliadorCercas.avaliar(null, listOf(dentro(), dentro("XYZ9876")), listOf(soUma))
        val (_, t2) = AvaliadorCercas.avaliar(s1, listOf(fora(), fora("XYZ9876")), listOf(soUma))
        assertEquals(listOf("ABC1D23"), t2.map { it.placa })
    }

    @Test
    fun `veiculo novo na frota nao dispara alerta na primeira vez que aparece`() {
        val (e1, _) = AvaliadorCercas.avaliar(null, listOf(dentro()), listOf(cerca))
        val (_, t) = AvaliadorCercas.avaliar(e1, listOf(dentro(), fora("NOV0A00")), listOf(cerca))
        assertTrue(t.isEmpty())
    }

    @Test
    fun `distancia de haversine`() {
        // 0,01 grau de latitude ≈ 1,11 km
        val d = AvaliadorCercas.distanciaMetros(-21.815, -48.2048, -21.825, -48.2048)
        assertEquals(1112.0, d, 5.0)
    }
}
