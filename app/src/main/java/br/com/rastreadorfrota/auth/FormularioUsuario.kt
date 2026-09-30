package br.com.rastreadorfrota.auth

enum class CampoUsuario {
    NOME, EMAIL, SENHA, CONFIRMACAO_SENHA, CPF, TELEFONE,
    CNH, CATEGORIA_CNH, VALIDADE_CNH, EMPRESA
}

/**
 * Estado do formulário de usuário, usado tanto no "Cadastrar-se" quanto no
 * "Meu perfil". No perfil, email, senha e perfil não são editáveis, por isso
 * a validação recebe [comCredenciais].
 */
data class FormularioUsuario(
    val nome: String = "",
    val email: String = "",
    val senha: String = "",
    val confirmacaoSenha: String = "",
    val cpf: String = "",
    val telefone: String = "",
    val perfil: Perfil = Perfil.MOTORISTA,
    val cnh: String = "",
    val categoriaCnh: String = "",
    val validadeCnh: String = "",
    val empresa: String = "",
    val cargo: String = ""
) {
    fun validar(comCredenciais: Boolean): Map<CampoUsuario, String> = buildMap {
        fun checar(campo: CampoUsuario, erro: String?) { if (erro != null) put(campo, erro) }

        checar(CampoUsuario.NOME, ValidacaoUsuario.nome(nome))
        checar(CampoUsuario.CPF, ValidacaoUsuario.cpf(cpf))
        checar(CampoUsuario.TELEFONE, ValidacaoUsuario.telefone(telefone))
        if (comCredenciais) {
            checar(CampoUsuario.EMAIL, ValidacaoUsuario.email(email))
            checar(CampoUsuario.SENHA, ValidacaoUsuario.senha(senha))
            checar(CampoUsuario.CONFIRMACAO_SENHA, ValidacaoUsuario.confirmacaoSenha(senha, confirmacaoSenha))
        }
        when (perfil) {
            Perfil.MOTORISTA -> {
                checar(CampoUsuario.CNH, ValidacaoUsuario.cnh(cnh))
                checar(CampoUsuario.CATEGORIA_CNH, ValidacaoUsuario.categoriaCnh(categoriaCnh))
                checar(CampoUsuario.VALIDADE_CNH, ValidacaoUsuario.validadeCnh(validadeCnh))
            }
            Perfil.CONTROLADOR -> checar(CampoUsuario.EMPRESA, ValidacaoUsuario.empresa(empresa))
        }
    }

    // Só grava os campos do perfil escolhido, pra não sobrar CNH num
    // controlador (ou empresa num motorista) se a pessoa trocou no meio.
    fun paraUsuario(uid: String): Usuario {
        val motorista = perfil == Perfil.MOTORISTA
        return Usuario(
            uid = uid,
            nome = nome.trim(),
            email = email.trim(),
            cpf = ValidacaoUsuario.apenasDigitos(cpf),
            telefone = ValidacaoUsuario.apenasDigitos(telefone),
            perfil = perfil,
            cnh = if (motorista) ValidacaoUsuario.apenasDigitos(cnh) else "",
            categoriaCnh = if (motorista) categoriaCnh else "",
            validadeCnh = if (motorista) validadeCnh.trim() else "",
            empresa = if (motorista) "" else empresa.trim(),
            cargo = if (motorista) "" else cargo.trim()
        )
    }

    companion object {
        fun de(usuario: Usuario) = FormularioUsuario(
            nome = usuario.nome,
            email = usuario.email,
            cpf = usuario.cpf,
            telefone = usuario.telefone,
            perfil = usuario.perfil,
            cnh = usuario.cnh,
            categoriaCnh = usuario.categoriaCnh,
            validadeCnh = usuario.validadeCnh,
            empresa = usuario.empresa,
            cargo = usuario.cargo
        )
    }
}
