package br.com.rastreadorfrota.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.data.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

sealed interface EstadoSessao {
    /** Ainda descobrindo se há alguém logado (a splash do sistema fica na tela). */
    data object Carregando : EstadoSessao
    data class Deslogado(val aviso: String? = null) : EstadoSessao
    data class Logado(val usuario: Usuario) : EstadoSessao
}

/**
 * Sessão no escopo da Activity: o usuário e o perfil são carregados UMA vez e compartilhados
 * por todas as telas (antes, cada tela relia o perfil no Firestore).
 */
class SessaoViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val app get() = getApplication<RastreadorApp>()

    private val _estado = MutableStateFlow<EstadoSessao>(EstadoSessao.Carregando)
    val estado: StateFlow<EstadoSessao> = _estado.asStateFlow()

    val usuario: Usuario? get() = (_estado.value as? EstadoSessao.Logado)?.usuario

    init {
        // Sessão já existente (app reaberto): entra direto, sem pedir a senha de novo.
        if (authRepository.currentUser == null) {
            _estado.value = EstadoSessao.Deslogado()
        } else {
            viewModelScope.launch { resolverSessao() }
        }

        // O gestor associa veículo e ativa/desativa o motorista em outro aparelho: a cada
        // sincronização concluída, relê o próprio perfil para refletir a mudança.
        viewModelScope.launch {
            SyncEngine.status
                .filterIsInstance<SyncStatus.Success>()
                .distinctUntilChangedBy { it.instante }
                .collect { if (_estado.value is EstadoSessao.Logado) recarregar() }
        }
    }

    private suspend fun resolverSessao() {
        _estado.value = try {
            // Sem resposta (rede muito ruim), a splash não pode ficar presa: volta ao login com aviso.
            val atual = withTimeoutOrNull(TIMEOUT_SESSAO_MS) { authRepository.buscarUsuarioAtual() ?: SEM_PERFIL }
            when {
                atual == null -> EstadoSessao.Deslogado("Não conseguimos carregar seu perfil. Verifique a conexão e entre de novo.")
                atual === SEM_PERFIL -> {
                    authRepository.logout()
                    EstadoSessao.Deslogado("Conta sem perfil cadastrado. Fale com o administrador.")
                }
                atual.perfil == Perfil.MOTORISTA && !atual.ativo -> {
                    authRepository.logout()
                    EstadoSessao.Deslogado("Sua conta de motorista foi desativada pelo gestor.")
                }
                else -> {
                    SessaoApp.iniciar(app, atual)
                    EstadoSessao.Logado(atual)
                }
            }
        } catch (e: Exception) {
            EstadoSessao.Deslogado(mensagemErroFirebase(e))
        }
    }

    /** Login ou cadastro concluído na tela. */
    fun entrou(usuario: Usuario) {
        SessaoApp.iniciar(app, usuario)
        _estado.value = EstadoSessao.Logado(usuario)
    }

    /** Perfil editado em "Meu perfil". */
    fun atualizar(usuario: Usuario) {
        SessaoApp.atualizar(usuario)
        _estado.value = EstadoSessao.Logado(usuario)
    }

    private fun recarregar() {
        viewModelScope.launch {
            val atual = runCatching { authRepository.buscarUsuarioAtual() }.getOrNull() ?: return@launch
            if (atual.perfil == Perfil.MOTORISTA && !atual.ativo) {
                sair("Sua conta de motorista foi desativada pelo gestor.")
            } else if (atual != usuario) {
                atualizar(atual)
            }
        }
    }

    private companion object {
        const val TIMEOUT_SESSAO_MS = 8_000L

        /** Marcador de "login existe, mas não há documento de perfil no Firestore". */
        val SEM_PERFIL = Usuario(uid = "", nome = "", email = "", cpf = "", telefone = "", perfil = Perfil.MOTORISTA)
    }

    fun sair(aviso: String? = null) {
        viewModelScope.launch {
            SessaoApp.encerrar(app)
            authRepository.logout()
            _estado.value = EstadoSessao.Deslogado(aviso)
        }
    }
}
