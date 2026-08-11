package br.com.rastreadorfrota.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.launch
import java.io.IOException

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
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
                uiState = LoginUiState.Success
            } catch (e: Exception) {
                uiState = LoginUiState.Error(mensagemDeErro(e))
            }
        }
    }

    private fun mensagemDeErro(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException ->
                "Email ou senha incorretos."
            is FirebaseAuthInvalidUserException ->
                "Não existe usuário cadastrado com esse email."
            is FirebaseAuthUserCollisionException ->
                "Já existe uma conta com esse email."
            is IOException ->
                "Sem conexão com a internet. Verifique sua rede e tente novamente."
            else ->
                "Não foi possível entrar. Tente novamente em instantes."
        }
    }
}