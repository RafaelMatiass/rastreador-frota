package br.com.rastreadorfrota.data.repository

import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Regras de negócio puras (sem Android), cobertas por testes unitários. */
object RegrasFrota {

    private val PLACA_ANTIGA = Regex("^[A-Z]{3}[0-9]{4}$")      // ABC1234
    private val PLACA_MERCOSUL = Regex("^[A-Z]{3}[0-9][A-Z][0-9]{2}$") // ABC1D23

    /** "abc-1d23 " -> "ABC1D23": tira traço e espaço, deixa maiúscula. */
    fun normalizarPlaca(placa: String): String =
        placa.uppercase().filter { it.isLetterOrDigit() }.take(7)

    /** null = válida; senão, a mensagem de erro para o campo. */
    fun validarPlaca(placa: String): String? {
        val p = normalizarPlaca(placa)
        return when {
            p.isEmpty() -> "Informe a placa."
            p.length < 7 -> "A placa tem 7 caracteres (ex.: ABC1D23)."
            !PLACA_ANTIGA.matches(p) && !PLACA_MERCOSUL.matches(p) -> "Placa inválida. Use ABC1234 ou ABC1D23."
            else -> null
        }
    }

    /**
     * Quem já está com o [veiculoId] (fora o próprio [uid]). Se existir, a atribuição vira
     * uma transferência e a tela pede confirmação, porque um veículo só tem um motorista.
     */
    fun motoristaComVeiculo(motoristas: List<MotoristaEntity>, veiculoId: Long, uid: String): MotoristaEntity? =
        motoristas.firstOrNull { it.veiculoId == veiculoId && it.uid != uid }

    /**
     * Dados legados podem ter dois motoristas com o mesmo veículo. Mantém o primeiro em ordem
     * alfabética e devolve os que precisam ficar sem veículo.
     */
    fun duplicadosParaSoltar(motoristas: List<MotoristaEntity>): List<MotoristaEntity> =
        motoristas.filter { it.veiculoId != null }
            .groupBy { it.veiculoId }
            .values
            .flatMap { mesmos -> mesmos.sortedBy { it.nome.lowercase() }.drop(1) }

    private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** Dias até a CNH vencer (negativo = vencida); null se a data não puder ser lida. */
    fun diasParaVencerCnh(validade: String, hoje: LocalDate = LocalDate.now()): Long? =
        runCatching { ChronoUnit.DAYS.between(hoje, LocalDate.parse(validade.trim(), FORMATO_DATA)) }.getOrNull()
}
