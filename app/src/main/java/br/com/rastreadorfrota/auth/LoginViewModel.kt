package br.com.rastreadorfrota.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val usuario: Usuario) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/** Login por email e senha. A sessão já existente (app reaberto) é tratada pelo SessaoViewModel. */
class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var uiState: LoginUiState by mutableStateOf(LoginUiState.Idle)
        private set

    /** Resultado do "Esqueci minha senha" (mensagem de sucesso ou erro). */
    var mensagemRecuperacao by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) {
        email = value
        if (uiState is LoginUiState.Error) uiState = LoginUiState.Idle
    }

    fun onPasswordChange(value: String) {
        password = value
        if (uiState is LoginUiState.Error) uiState = LoginUiState.Idle
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error("Preencha email e senha.")
            return
        }
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            uiState = try {
                authRepository.login(email.trim(), password)
                val usuario = authRepository.buscarUsuarioAtual()
                when {
                    usuario == null -> {
                        // Login existe no Auth, mas sem documento/perfil no Firestore.
                        authRepository.logout()
                        LoginUiState.Error("Conta sem perfil cadastrado. Fale com o administrador.")
                    }
                    usuario.perfil == Perfil.MOTORISTA && !usuario.ativo -> {
                        authRepository.logout()
                        LoginUiState.Error("Sua conta de motorista foi desativada pelo gestor.")
                    }
                    else -> LoginUiState.Success(usuario)
                }
            } catch (e: Exception) {
                LoginUiState.Error(mensagemErroFirebase(e))
            }
        }
    }

    fun recuperarSenha() {
        if (ValidacaoUsuario.email(email) != null) {
            mensagemRecuperacao = "Digite seu email no campo acima para receber o link."
            return
        }
        viewModelScope.launch {
            mensagemRecuperacao = try {
                authRepository.enviarRecuperacaoSenha(email.trim())
                "Enviamos um link de redefinição para ${email.trim()}."
            } catch (e: Exception) {
                mensagemErroFirebase(e)
            }
        }
    }

    fun dispensarMensagemRecuperacao() {
        mensagemRecuperacao = null
    }
}
