package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.connectivity.ConnectivityObserver
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.sync.FirestoreSyncManager
import br.com.rastreadorfrota.data.sync.SyncTrigger
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout

// Timeout de segurança: se o Firestore não responder nesse prazo (rede
// instável, regras bloqueando, projeto mal configurado), a sincronização é
// cancelada e reportada como erro em vez de girar pra sempre.
private const val TIMEOUT_SYNC_MS = 15_000L

// "Batimento" de sincronização automática em segundo plano, além dos
// gatilhos por evento (conexão voltou / dado salvo). Cobre mudanças feitas
// em outro aparelho ou direto no Firestore.
private const val AUTO_SYNC_INTERVAL_MS = 60_000L

// Agrupa salvamentos feitos em sequência (ex: cadastrar 3 veículos rápido)
// numa única sincronização, em vez de disparar uma pra cada um.
private const val TRIGGER_DEBOUNCE_MS = 1_500L

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

    // Garante que só existe UMA sincronização em andamento por vez.
    private val syncMutex = Mutex()

    val isOnline: StateFlow<Boolean> = connectivityObserver.observar()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = false)

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var ultimoEstadoOnline = false

    init {
        // Gatilho 1: a conexão voltou → sincroniza.
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online && !ultimoEstadoOnline) {
                    sincronizarAgora()
                }
                ultimoEstadoOnline = online
            }
        }

        // Gatilho 2: algo foi salvo/removido (veículo ou motorista, em
        // qualquer tela) → sincroniza automaticamente.
        viewModelScope.launch {
            SyncTrigger.requests
                .debounce(TRIGGER_DEBOUNCE_MS)
                .collect { sincronizarAgora() }
        }

        // Gatilho 3: rede de segurança — a cada 60s, se estiver online,
        // sincroniza de qualquer forma.
        viewModelScope.launch {
            while (true) {
                delay(AUTO_SYNC_INTERVAL_MS)
                if (isOnline.value) sincronizarAgora()
            }
        }
    }

    fun sincronizarAgora() {
        // tryLock: se já tem sync rodando, essa chamada desiste sem criar
        // condição de corrida — importante já que agora várias fontes
        // podem pedir sync ao mesmo tempo.
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