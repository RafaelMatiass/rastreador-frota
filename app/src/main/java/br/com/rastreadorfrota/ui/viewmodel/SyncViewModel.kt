package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.connectivity.ConnectivityObserver
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.sync.FirestoreSyncManager
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

// Timeout de segurança: se o Firestore não responder nesse prazo (rede
// instável, regras bloqueando, projeto mal configurado), a sincronização é
// cancelada e reportada como erro em vez de girar pra sempre.
private const val TIMEOUT_SYNC_MS = 15_000L

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val log: List<String>) : SyncStatus
    data class Error(val mensagem: String) : SyncStatus
}

class SyncViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val syncManager = FirestoreSyncManager(db.veiculoDao(), db.motoristaDao())
    private val connectivityObserver = ConnectivityObserver(application)

    // Garante que só existe UMA sincronização em andamento por vez, de forma
    // atômica — evita a corrida que deixava o spinner preso.
    private val syncMutex = Mutex()

    val isOnline: StateFlow<Boolean> = connectivityObserver.observar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var ultimoEstadoOnline = false

    init {
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online && !ultimoEstadoOnline) {
                    sincronizarAgora()
                }
                ultimoEstadoOnline = online
            }
        }
    }

    fun sincronizarAgora() {
        // tryLock em vez de checar um Boolean solto: se já tem sync rodando,
        // essa chamada simplesmente desiste, sem condição de corrida.
        if (!syncMutex.tryLock()) return

        viewModelScope.launch {
            _status.value = SyncStatus.Syncing
            try {
                val resultado = withTimeout(TIMEOUT_SYNC_MS) {
                    syncManager.sincronizarTudo()
                }
                _status.value = SyncStatus.Success(resultado.log)
            } catch (e: TimeoutCancellationException) {
                _status.value = SyncStatus.Error(
                    "Tempo esgotado (${TIMEOUT_SYNC_MS / 1000}s). Verifique a conexão e as regras do Firestore."
                )
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(e.message ?: "Erro desconhecido ao sincronizar.")
            } finally {
                syncMutex.unlock()
            }
        }
    }
}