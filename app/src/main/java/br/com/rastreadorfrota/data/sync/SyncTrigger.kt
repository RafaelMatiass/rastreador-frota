package br.com.rastreadorfrota.data.sync

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Canal simples para pedir uma sincronização de qualquer lugar do app
 * (ex: depois de salvar um veículo ou motorista), sem acoplar os
 * ViewModels de cadastro ao SyncViewModel diretamente.
 */
object SyncTrigger {
    private val _requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requests: SharedFlow<Unit> = _requests

    fun requestSync() {
        _requests.tryEmit(Unit)
    }
}