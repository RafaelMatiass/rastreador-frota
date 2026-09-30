package br.com.rastreadorfrota.simulacao

/**
 * Monta uma sequência PRÉ-DETERMINADA de pontos a partir de trechos e paradas
 * (nada é aleatório: a mesma rota gera sempre a mesma viagem).
 * Andando: motor ligado e portas fechadas. Em cada parada a telemetria é informada.
 */
private class Rota(lat: Double, lon: Double) {
    private val pontos = mutableListOf<PontoTelemetria>()
    private var atual = lat to lon

    /** Anda em linha reta até (lat, lon) em [passos] pontos, a [kmh]. */
    fun trecho(lat: Double, lon: Double, passos: Int, kmh: Int) = apply {
        val (lat0, lon0) = atual
        for (i in 1..passos) {
            val f = i.toDouble() / passos
            pontos += PontoTelemetria(lat0 + (lat - lat0) * f, lon0 + (lon - lon0) * f, kmh, true, false)
        }
        atual = lat to lon
    }

    /** Semáforo/trânsito: parado com motor ligado e portas fechadas. */
    fun semaforo(passos: Int) = parada(passos, motorLigado = true, portasAbertas = false)

    /** Entrega: motor desligado e portas abertas. */
    fun entrega(passos: Int) = parada(passos, motorLigado = false, portasAbertas = true)

    private fun parada(passos: Int, motorLigado: Boolean, portasAbertas: Boolean) = apply {
        val (lat, lon) = atual
        repeat(passos) { pontos += PontoTelemetria(lat, lon, 0, motorLigado, portasAbertas) }
    }

    fun pontos(): List<PontoTelemetria> = pontos.toList()
}

/** Viagens circulares em Araraquara/SP: cada uma termina onde começou e se repete. */
internal val rotasSimuladas: List<List<PontoTelemetria>> = listOf(
    // Centro -> Jardim Primavera -> Centro
    Rota(-21.7946, -48.1756)
        .trecho(-21.8009, -48.1682, 4, 42)
        .semaforo(2)
        .trecho(-21.8072, -48.1604, 4, 36)
        .entrega(4)
        .trecho(-21.8010, -48.1700, 3, 45)
        .trecho(-21.7946, -48.1756, 3, 40)
        .semaforo(1)
        .pontos(),

    // Vila Xavier -> Selmi Dei -> Vila Xavier
    Rota(-21.7780, -48.1850)
        .trecho(-21.7700, -48.1760, 5, 38)
        .entrega(3)
        .trecho(-21.7760, -48.1650, 4, 45)
        .semaforo(2)
        .trecho(-21.7780, -48.1850, 6, 40)
        .pontos(),

    // Centro de distribuição (Rod. Washington Luís) -> Centro -> CD
    Rota(-21.8150, -48.2050)
        .trecho(-21.8050, -48.1950, 4, 60)
        .trecho(-21.7950, -48.1850, 4, 48)
        .semaforo(1)
        .trecho(-21.7880, -48.1780, 3, 30)
        .entrega(5)
        .trecho(-21.8000, -48.1900, 4, 50)
        .trecho(-21.8150, -48.2050, 4, 62)
        .entrega(2) // descarga no CD
        .pontos(),

    // Vale do Sol -> Jardim Morumbi -> Vale do Sol
    Rota(-21.7700, -48.1500)
        .trecho(-21.7800, -48.1450, 4, 40)
        .semaforo(2)
        .trecho(-21.7900, -48.1500, 3, 35)
        .entrega(3)
        .trecho(-21.7820, -48.1580, 3, 42)
        .trecho(-21.7700, -48.1500, 3, 38)
        .pontos()
)
