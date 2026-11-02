package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RegrasFrotaTest {

    private fun motorista(uid: String, nome: String, veiculoId: Long?) =
        MotoristaEntity(uid, nome, "$uid@x.com", "", "123", "C", "", veiculoId)

    @Test
    fun `placa antiga e Mercosul sao validas, com ou sem traco`() {
        assertNull(RegrasFrota.validarPlaca("ABC1234"))
        assertNull(RegrasFrota.validarPlaca("abc-1d23"))
        assertEquals("ABC1D23", RegrasFrota.normalizarPlaca(" abc-1d23 "))
    }

    @Test
    fun `placa invalida tem mensagem`() {
        assertNotNull(RegrasFrota.validarPlaca(""))
        assertNotNull(RegrasFrota.validarPlaca("AB12"))
        assertNotNull(RegrasFrota.validarPlaca("1234ABC"))
        assertNotNull(RegrasFrota.validarPlaca("ABCD123"))
    }

    @Test
    fun `atribuir veiculo de outro motorista vira transferencia`() {
        val joao = motorista("1", "João", 10)
        val ana = motorista("2", "Ana", null)
        val lista = listOf(joao, ana)
        assertEquals(joao, RegrasFrota.motoristaComVeiculo(lista, 10, ana.uid))
        // O próprio dono não conta como conflito, e veículo livre não tem ninguém.
        assertNull(RegrasFrota.motoristaComVeiculo(lista, 10, joao.uid))
        assertNull(RegrasFrota.motoristaComVeiculo(lista, 99, ana.uid))
    }

    @Test
    fun `dados legados com veiculo duplicado mantem um motorista so`() {
        val lista = listOf(motorista("1", "Carlos", 5), motorista("2", "Ana", 5), motorista("3", "Bia", 7), motorista("4", "Davi", null))
        val soltar = RegrasFrota.duplicadosParaSoltar(lista)
        assertEquals(listOf("Carlos"), soltar.map { it.nome })
    }

    @Test
    fun `dias para vencer a CNH`() {
        val hoje = LocalDate.of(2026, 11, 2)
        assertEquals(10L, RegrasFrota.diasParaVencerCnh("12/11/2026", hoje))
        assertEquals(-2L, RegrasFrota.diasParaVencerCnh("31/10/2026", hoje))
        assertNull(RegrasFrota.diasParaVencerCnh("data ruim", hoje))
    }
}
