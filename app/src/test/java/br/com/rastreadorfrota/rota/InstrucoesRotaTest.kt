package br.com.rastreadorfrota.rota

import org.junit.Assert.assertEquals
import org.junit.Test

class InstrucoesRotaTest {

    @Test
    fun `curvas viram frases em portugues com o nome da rua`() {
        assertEquals("Vire à direita na Avenida Barroso" to Manobra.DIREITA, InstrucoesRota.traduzir("turn", "right", "Avenida Barroso"))
        assertEquals("Vire à esquerda" to Manobra.ESQUERDA, InstrucoesRota.traduzir("turn", "left", null))
        assertEquals("Mantenha-se à direita na Rua 9" to Manobra.LEVE_DIREITA, InstrucoesRota.traduzir("turn", "slight right", "Rua 9"))
        assertEquals("Faça o retorno" to Manobra.RETORNO, InstrucoesRota.traduzir("turn", "uturn", ""))
    }

    @Test
    fun `saida, chegada e rotatoria`() {
        assertEquals("Siga pela Rua Voluntários da Pátria" to Manobra.SAIDA, InstrucoesRota.traduzir("depart", null, "Rua Voluntários da Pátria"))
        assertEquals("Você chegou ao destino" to Manobra.CHEGADA, InstrucoesRota.traduzir("arrive", null, "Qualquer"))
        assertEquals(
            "Na rotatória, saia na 2ª saída na Avenida Bento de Abreu" to Manobra.ROTATORIA,
            InstrucoesRota.traduzir("roundabout", "right", "Avenida Bento de Abreu", 2)
        )
    }

    @Test
    fun `continuar reto e bifurcacao`() {
        assertEquals("Continue em frente pela Rua 2" to Manobra.RETO, InstrucoesRota.traduzir("continue", "straight", "Rua 2"))
        assertEquals("Na bifurcação, mantenha-se à esquerda na SP-255" to Manobra.LEVE_ESQUERDA, InstrucoesRota.traduzir("fork", "slight left", "SP-255"))
    }
}
