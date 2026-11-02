package br.com.rastreadorfrota.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Aparência escolhida pelo usuário: seguir o aparelho ou forçar claro/escuro. */
enum class TemaApp(val rotulo: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    ESCURO("Escuro")
}

/**
 * Preferência de tema do app, salva no aparelho e observável: trocar no botão
 * muda todas as telas na hora, sem reiniciar.
 */
object PreferenciaTema {
    private const val PREFS = "traksync_prefs"
    private const val CHAVE = "tema"

    private val _tema = MutableStateFlow(TemaApp.SISTEMA)
    val tema: StateFlow<TemaApp> = _tema.asStateFlow()

    fun carregar(context: Context) {
        val salvo = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CHAVE, null)
        _tema.value = TemaApp.entries.find { it.name == salvo } ?: TemaApp.SISTEMA
    }

    fun definir(context: Context, tema: TemaApp) {
        _tema.value = tema
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(CHAVE, tema.name) }
    }
}

/** true se a tela deve ficar escura agora (considera a escolha do usuário e o aparelho). */
@Composable
fun temaEscuroAtivo(): Boolean {
    val tema by PreferenciaTema.tema.collectAsState()
    return when (tema) {
        TemaApp.SISTEMA -> isSystemInDarkTheme()
        TemaApp.CLARO -> false
        TemaApp.ESCURO -> true
    }
}
