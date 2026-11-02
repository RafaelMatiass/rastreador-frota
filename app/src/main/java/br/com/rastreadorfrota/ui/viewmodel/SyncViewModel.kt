package br.com.rastreadorfrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.data.sync.SyncStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Casca fina sobre o [SyncEngine], que vive no escopo do app.
 * Criar este ViewModel em várias telas não cria vários loops de sincronização.
 */
class SyncViewModel : ViewModel() {
    val isOnline: StateFlow<Boolean> = SyncEngine.isOnline
    val status: StateFlow<SyncStatus> = SyncEngine.status
    val pendentes: StateFlow<Int> = SyncEngine.pendentes

    fun sincronizarAgora() = SyncEngine.sincronizarAgora()
}
