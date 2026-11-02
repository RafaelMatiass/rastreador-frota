package br.com.rastreadorfrota.rota

/** Tipo de manobra, para a tela escolher o ícone (seta para a direita, rotatória...). */
enum class Manobra { SAIDA, CHEGADA, RETO, DIREITA, ESQUERDA, LEVE_DIREITA, LEVE_ESQUERDA, RETORNO, ROTATORIA }

/** Um passo da rota: "Vire à direita na Avenida Barroso", com a distância até o próximo passo. */
data class InstrucaoRota(
    val texto: String,
    val distanciaMetros: Double,
    val manobra: Manobra
)

/**
 * O OSRM devolve cada manobra como dados ("turn" + "right" + nome da rua), sem texto em
 * português. Aqui esses dados viram a frase que aparece no passo a passo.
 * Função pura: coberta por testes unitários.
 */
object InstrucoesRota {

    fun traduzir(tipo: String, modificador: String?, rua: String?, saidaRotatoria: Int? = null): Pair<String, Manobra> {
        val naRua = rua?.takeIf { it.isNotBlank() }?.let { " na $it" }.orEmpty()
        val pelaRua = rua?.takeIf { it.isNotBlank() }?.let { " pela $it" }.orEmpty()
        val manobra = manobraDe(modificador)
        return when (tipo) {
            "depart" -> "Siga$pelaRua" to Manobra.SAIDA
            "arrive" -> "Você chegou ao destino" to Manobra.CHEGADA
            "roundabout", "rotary", "roundabout turn" -> {
                val saida = saidaRotatoria?.let { "na ${it}ª saída" } ?: "na saída indicada"
                "Na rotatória, saia $saida$naRua" to Manobra.ROTATORIA
            }
            "exit roundabout", "exit rotary" -> "Saia da rotatória$naRua" to Manobra.ROTATORIA
            "continue", "new name" ->
                if (manobra == Manobra.RETO) "Continue em frente$pelaRua" to Manobra.RETO
                else "${verbo(manobra)}$naRua" to manobra
            "merge" -> "Entre$naRua" to manobra
            "on ramp" -> "Pegue o acesso$naRua" to manobra
            "off ramp" -> "Pegue a saída$naRua" to manobra
            "fork" -> "Na bifurcação, mantenha-se ${lado(manobra)}$naRua" to manobra
            "end of road" -> "No fim da via, ${verbo(manobra).lowercase()}$naRua" to manobra
            else -> // "turn" e qualquer tipo novo
                if (manobra == Manobra.RETO) "Siga em frente$pelaRua" to Manobra.RETO
                else "${verbo(manobra)}$naRua" to manobra
        }
    }

    private fun manobraDe(modificador: String?): Manobra = when (modificador) {
        "right", "sharp right" -> Manobra.DIREITA
        "left", "sharp left" -> Manobra.ESQUERDA
        "slight right" -> Manobra.LEVE_DIREITA
        "slight left" -> Manobra.LEVE_ESQUERDA
        "uturn" -> Manobra.RETORNO
        else -> Manobra.RETO
    }

    private fun verbo(m: Manobra) = when (m) {
        Manobra.DIREITA -> "Vire à direita"
        Manobra.ESQUERDA -> "Vire à esquerda"
        Manobra.LEVE_DIREITA -> "Mantenha-se à direita"
        Manobra.LEVE_ESQUERDA -> "Mantenha-se à esquerda"
        Manobra.RETORNO -> "Faça o retorno"
        else -> "Siga em frente"
    }

    private fun lado(m: Manobra) = when (m) {
        Manobra.DIREITA, Manobra.LEVE_DIREITA -> "à direita"
        Manobra.ESQUERDA, Manobra.LEVE_ESQUERDA -> "à esquerda"
        else -> "em frente"
    }
}
