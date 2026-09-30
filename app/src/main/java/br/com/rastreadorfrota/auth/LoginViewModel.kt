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
    data class Success(val perfil: Perfil) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var email by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var uiState: LoginUiState by mutableStateOf(LoginUiState.Idle)
        private set

    init {
        // Sessão já existente (app reaberto): pula o formulário e vai direto
        // pra tela do perfil, sem pedir a senha de novo.
        if (authRepository.currentUser != null) {
            uiState = LoginUiState.Loading
            viewModelScope.launch { entrarComPerfil() }
        }
    }

    fun onEmailChange(value: String) {
        email = value
    }

    fun onPasswordChange(value: String) {
        password = value
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            uiState = LoginUiState.Error("Preencha email e senha.")
            return
        }
        uiState = LoginUiState.Loading
        viewModelScope.launch {
            try {
                authRepository.login(email.trim(), password)
                entrarComPerfil()
            } catch (e: Exception) {
                android.util.Log.e("LoginDebug", "Erro no login", e)
                uiState = LoginUiState.Error(mensagemErroFirebase(e))
            }
        }
    }

    private suspend fun entrarComPerfil() {
        uiState = try {
            val usuario = authRepository.buscarUsuarioAtual()
            if (usuario != null && usuario.perfil == Perfil.MOTORISTA && !usuario.ativo) {
                authRepository.logout()
                LoginUiState.Error("Sua conta de motorista foi desativada pelo controlador.")
            } else if (usuario != null) {
                LoginUiState.Success(usuario.perfil)
            } else {
                // Login existe no Auth, mas sem documento/perfil no Firestore.
                authRepository.logout()
                LoginUiState.Error("Conta sem perfil cadastrado. Fale com o administrador.")
            }
        } catch (e: Exception) {
            android.util.Log.e("LoginDebug", "Erro ao carregar perfil", e)
            LoginUiState.Error(mensagemErroFirebase(e))
        }
    }
}
