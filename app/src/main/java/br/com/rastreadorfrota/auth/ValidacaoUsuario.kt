package br.com.rastreadorfrota.auth

import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Regras de validação compartilhadas entre o "Cadastrar-se" e o "Meu perfil".
 * Cada função devolve a mensagem de erro, ou null se o valor é válido.
 */
object ValidacaoUsuario {

    fun nome(valor: String): String? =
        if (valor.trim().split(" ").filter { it.isNotBlank() }.size < 2) "Informe nome e sobrenome." else null

    fun email(valor: String): String? =
        if (!Patterns.EMAIL_ADDRESS.matcher(valor.trim()).matches()) "Email inválido." else null

    fun senha(valor: String): String? =
        if (valor.length < 6) "A senha precisa ter pelo menos 6 caracteres." else null

    fun confirmacaoSenha(senha: String, confirmacao: String): String? =
        if (senha != confirmacao) "As senhas não conferem." else null

    fun telefone(valor: String): String? =
        if (apenasDigitos(valor).length !in 10..11) "Telefone deve ter DDD + número." else null

    fun cpf(valor: String): String? =
        if (!cpfValido(apenasDigitos(valor))) "CPF inválido." else null

    fun cnh(valor: String): String? =
        if (apenasDigitos(valor).length != 11) "A CNH deve ter 11 dígitos." else null

    fun categoriaCnh(valor: String): String? =
        if (CategoriaCnh.entries.none { it.name == valor }) "Selecione a categoria da CNH." else null

    fun validadeCnh(valor: String): String? {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val data = runCatching { formato.parse(valor.trim()) }.getOrNull()
            ?: return "Use o formato dd/mm/aaaa."
        return if (data.before(java.util.Date())) "A CNH está vencida." else null
    }

    fun empresa(valor: String): String? =
        if (valor.isBlank()) "Informe a empresa." else null

    fun apenasDigitos(valor: String) = valor.filter(Char::isDigit)

    private fun cpfValido(cpf: String): Boolean {
        if (cpf.length != 11 || cpf.all { it == cpf[0] }) return false
        val digitos = cpf.map { it - '0' }
        fun digitoVerificador(quantidade: Int): Int {
            val soma = (0 until quantidade).sumOf { digitos[it] * (quantidade + 1 - it) }
            val resto = (soma * 10) % 11
            return if (resto == 10) 0 else resto
        }
        return digitoVerificador(9) == digitos[9] && digitoVerificador(10) == digitos[10]
    }
}
