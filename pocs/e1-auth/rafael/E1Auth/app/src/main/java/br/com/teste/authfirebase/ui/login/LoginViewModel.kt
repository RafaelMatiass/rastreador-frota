package br.com.teste.authfirebase.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.teste.authfirebase.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // se já tinha uma sessão Firebase ativa, entra direto
        if (repository.usuarioAtual() != null) {
            _uiState.value = _uiState.value.copy(isAuthenticated = true)
        }
    }

    fun onEmailChange(novoEmail: String) {
        _uiState.value = _uiState.value.copy(email = novoEmail, errorMessage = null)
    }

    fun onPasswordChange(novaSenha: String) {
        _uiState.value = _uiState.value.copy(password = novaSenha, errorMessage = null)
    }

    fun login() {
        val estadoAtual = _uiState.value

        if (estadoAtual.email.isBlank() || estadoAtual.password.isBlank()) {
            _uiState.value = estadoAtual.copy(errorMessage = "Preencha e-mail e senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val resultado = repository.login(estadoAtual.email, estadoAtual.password)

            resultado
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = true
                    )
                }
                .onFailure { erro ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        // Firebase manda mensagens em inglês; aqui simplificamos
                        errorMessage = traduzErro(erro.message)
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = LoginUiState()
    }

    private fun traduzErro(mensagemOriginal: String?): String {
        return when {
            mensagemOriginal == null -> "Erro desconhecido"
            mensagemOriginal.contains("badly formatted", ignoreCase = true) ->
                "E-mail em formato inválido"
            mensagemOriginal.contains("no user record", ignoreCase = true) ->
                "Usuário não cadastrado"
            mensagemOriginal.contains("password is invalid", ignoreCase = true) ->
                "Senha incorreta"
            mensagemOriginal.contains("network", ignoreCase = true) ->
                "Sem conexão com a internet"
            else -> mensagemOriginal
        }
    }
}