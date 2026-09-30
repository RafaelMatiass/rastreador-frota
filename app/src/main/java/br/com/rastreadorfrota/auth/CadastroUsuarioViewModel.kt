package br.com.rastreadorfrota.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface CadastroUsuarioUiState {
    data object Idle : CadastroUsuarioUiState
    data object Loading : CadastroUsuarioUiState
    data class Success(val perfil: Perfil) : CadastroUsuarioUiState
    data class Error(val message: String) : CadastroUsuarioUiState
}

class CadastroUsuarioViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var formulario by mutableStateOf(FormularioUsuario())
        private set

    var erros by mutableStateOf<Map<CampoUsuario, String>>(emptyMap())
        private set

    var uiState: CadastroUsuarioUiState by mutableStateOf(CadastroUsuarioUiState.Idle)
        private set

    fun onFormularioChange(novo: FormularioUsuario) {
        formulario = novo
        // Revalida só depois da primeira tentativa, pra não mostrar erro
        // enquanto a pessoa ainda está digitando pela primeira vez.
        if (erros.isNotEmpty()) erros = novo.validar(comCredenciais = true)
    }

    fun cadastrar() {
        erros = formulario.validar(comCredenciais = true)
        if (erros.isNotEmpty()) {
            uiState = CadastroUsuarioUiState.Error("Corrija os campos destacados.")
            return
        }
        uiState = CadastroUsuarioUiState.Loading
        viewModelScope.launch {
            uiState = try {
                val usuario = authRepository.cadastrar(formulario.paraUsuario(uid = ""), formulario.senha)
                CadastroUsuarioUiState.Success(usuario.perfil)
            } catch (e: Exception) {
                android.util.Log.e("CadastroDebug", "Erro no cadastro", e)
                CadastroUsuarioUiState.Error(mensagemErroFirebase(e))
            }
        }
    }
}
