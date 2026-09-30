package br.com.rastreadorfrota.auth

import com.google.firebase.firestore.DocumentSnapshot

enum class Perfil(val label: String) {
    MOTORISTA("Motorista"),
    CONTROLADOR("Controlador")
}

enum class CategoriaCnh { A, B, C, D, E, AB, AC, AD, AE }

/**
 * Dados do usuário guardados em "usuarios/{uid}" no Firestore.
 * Os campos de CNH só se aplicam ao motorista; empresa/cargo, ao controlador.
 *
 * Para o motorista, este documento é o ÚNICO registro dele no sistema: o
 * controlador não cadastra motoristas, só gerencia [veiculoRemoteId] e [ativo].
 *
 * A foto de perfil NÃO fica aqui: ela é salva apenas no armazenamento
 * interno do aparelho (ver FotoLocal).
 */
data class Usuario(
    val uid: String,
    val nome: String,
    val email: String,
    val cpf: String,
    val telefone: String,
    val perfil: Perfil,
    val cnh: String = "",
    val categoriaCnh: String = "",
    val validadeCnh: String = "",
    val empresa: String = "",
    val cargo: String = "",
    val veiculoRemoteId: String? = null,
    val ativo: Boolean = true
) {
    /** Dados cadastrais, que o próprio usuário preenche e edita. */
    fun toMap(): Map<String, Any> = mapOf(
        "nome" to nome,
        "email" to email,
        "cpf" to cpf,
        "telefone" to telefone,
        "perfil" to perfil.name,
        "cnh" to cnh,
        "categoriaCnh" to categoriaCnh,
        "validadeCnh" to validadeCnh,
        "empresa" to empresa,
        "cargo" to cargo
    )

    companion object {
        // Campos geridos pelo controlador (ficam fora do toMap de propósito,
        // pra edição do "Meu perfil" nunca sobrescrever o que ele definiu).
        const val CAMPO_VEICULO_REMOTE_ID = "veiculoRemoteId"
        const val CAMPO_ATIVO = "ativo"

        // Usuários antigos (criados à mão no console) podem não ter todos os
        // campos, então tudo além de "perfil" tem valor padrão.
        fun fromDocument(doc: DocumentSnapshot): Usuario? {
            val perfil = doc.getString("perfil")
                ?.let { valor -> Perfil.entries.find { it.name == valor.uppercase() } }
                ?: return null
            return Usuario(
                uid = doc.id,
                nome = doc.getString("nome") ?: "",
                email = doc.getString("email") ?: "",
                cpf = doc.getString("cpf") ?: "",
                telefone = doc.getString("telefone") ?: "",
                perfil = perfil,
                cnh = doc.getString("cnh") ?: "",
                categoriaCnh = doc.getString("categoriaCnh") ?: "",
                validadeCnh = doc.getString("validadeCnh") ?: "",
                empresa = doc.getString("empresa") ?: "",
                cargo = doc.getString("cargo") ?: "",
                veiculoRemoteId = doc.getString(CAMPO_VEICULO_REMOTE_ID),
                ativo = doc.getBoolean(CAMPO_ATIVO) ?: true
            )
        }
    }
}
