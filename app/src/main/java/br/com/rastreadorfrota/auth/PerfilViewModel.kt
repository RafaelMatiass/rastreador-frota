package br.com.rastreadorfrota.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// O Firestore guarda a escrita no cache local na hora, mas o await() só
// termina quando o servidor confirma. Offline isso nunca acontece, então
// depois desse prazo consideramos salvo localmente (o envio fica na fila).
private const val TIMEOUT_SALVAR_MS = 8_000L

sealed interface PerfilUiState {
    data object Carregando : PerfilUiState
    data class Pronto(val mensagem: String? = null, val ehErro: Boolean = false) : PerfilUiState
    data object Salvando : PerfilUiState
    data class Erro(val mensagem: String) : PerfilUiState
}

class PerfilViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var usuario by mutableStateOf<Usuario?>(null)
        private set

    var formulario by mutableStateOf(FormularioUsuario())
        private set

    var erros by mutableStateOf<Map<CampoUsuario, String>>(emptyMap())
        private set

    var uiState: PerfilUiState by mutableStateOf(PerfilUiState.Carregando)
        private set

    val uid: String? get() = authRepository.currentUser?.uid

    val houveAlteracao: Boolean
        get() = usuario?.let { it.comDadosDoFormulario() != it } ?: false

    init {
        carregar()
    }

    fun carregar() {
        uiState = PerfilUiState.Carregando
        viewModelScope.launch {
            uiState = try {
                val atual = authRepository.buscarUsuarioAtual()
                if (atual == null) {
                    PerfilUiState.Erro("Perfil não encontrado.")
                } else {
                    usuario = atual
                    formulario = FormularioUsuario.de(atual)
                    PerfilUiState.Pronto()
                }
            } catch (e: Exception) {
                PerfilUiState.Erro(mensagemErroFirebase(e))
            }
        }
    }

    fun onFormularioChange(novo: FormularioUsuario) {
        formulario = novo
        if (erros.isNotEmpty()) erros = novo.validar(comCredenciais = false)
        if (uiState is PerfilUiState.Pronto) uiState = PerfilUiState.Pronto()
    }

    fun descartarAlteracoes() {
        usuario?.let { formulario = FormularioUsuario.de(it) }
        erros = emptyMap()
        uiState = PerfilUiState.Pronto()
    }

    fun salvar() {
        val atual = usuario ?: return
        erros = formulario.validar(comCredenciais = false)
        if (erros.isNotEmpty()) {
            uiState = PerfilUiState.Pronto("Corrija os campos destacados.", ehErro = true)
            return
        }
        val atualizado = atual.comDadosDoFormulario()
        uiState = PerfilUiState.Salvando
        viewModelScope.launch {
            uiState = try {
                withTimeout(TIMEOUT_SALVAR_MS) { authRepository.atualizarUsuario(atualizado) }
                confirmarAlteracao(atualizado)
                PerfilUiState.Pronto("Dados atualizados.")
            } catch (e: TimeoutCancellationException) {
                confirmarAlteracao(atualizado)
                PerfilUiState.Pronto("Salvo no aparelho. Será enviado quando houver conexão.")
            } catch (e: Exception) {
                PerfilUiState.Pronto(mensagemErroFirebase(e), ehErro = true)
            }
        }
    }

    // O formulário só conhece os dados cadastrais; veículo e status vêm do
    // controlador e são mantidos como estão.
    private fun Usuario.comDadosDoFormulario() =
        formulario.paraUsuario(uid).copy(veiculoRemoteId = veiculoRemoteId, ativo = ativo)

    private fun confirmarAlteracao(atualizado: Usuario) {
        usuario = atualizado
        formulario = FormularioUsuario.de(atualizado)
    }

    fun logout() {
        authRepository.logout()
    }
}
