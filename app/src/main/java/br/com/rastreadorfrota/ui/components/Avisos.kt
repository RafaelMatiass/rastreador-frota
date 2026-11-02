package br.com.rastreadorfrota.ui.components

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Mensagem rápida no rodapé (snackbar), com ação opcional como "Desfazer". */
data class Aviso(val texto: String, val acao: String? = null, val onAcao: (() -> Unit)? = null)

/**
 * Canal único de avisos do app: qualquer tela ou ViewModel publica e o snackbar do
 * shell mostra, inclusive depois de voltar de uma tela cheia (ex.: "Veículo salvo").
 */
object Avisos {
    private val _fluxo = MutableSharedFlow<Aviso>(extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val fluxo: SharedFlow<Aviso> = _fluxo

    fun mostrar(texto: String, acao: String? = null, onAcao: (() -> Unit)? = null) {
        _fluxo.tryEmit(Aviso(texto, acao, onAcao))
    }
}
