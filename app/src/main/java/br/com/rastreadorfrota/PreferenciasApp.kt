package br.com.rastreadorfrota

import android.content.Context
import androidx.core.content.edit

/**
 * Preferências simples do aparelho (não são dados do negócio, então não vão pro Room
 * nem pro Firestore): se a introdução já foi vista e as últimas buscas de endereço.
 */
class PreferenciasApp(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("traksync_prefs", Context.MODE_PRIVATE)

    var onboardingVisto: Boolean
        get() = prefs.getBoolean(CHAVE_ONBOARDING, false)
        set(valor) = prefs.edit { putBoolean(CHAVE_ONBOARDING, valor) }

    /** Já explicamos (e pedimos) a permissão de notificações? Evita insistir a cada login. */
    var pediuNotificacoes: Boolean
        get() = prefs.getBoolean(CHAVE_NOTIFICACOES, false)
        set(valor) = prefs.edit { putBoolean(CHAVE_NOTIFICACOES, valor) }

    /** Últimas buscas do planejador de rotas, no formato "descricao|lat|lon", mais recente primeiro. */
    var buscasRecentes: List<String>
        get() = prefs.getString(CHAVE_BUSCAS, null)?.split("\n")?.filter { it.isNotBlank() }.orEmpty()
        set(valor) = prefs.edit { putString(CHAVE_BUSCAS, valor.take(MAX_BUSCAS).joinToString("\n")) }

    private companion object {
        const val CHAVE_ONBOARDING = "onboarding_visto"
        const val CHAVE_NOTIFICACOES = "pediu_notificacoes"
        const val CHAVE_BUSCAS = "buscas_recentes"
        const val MAX_BUSCAS = 5
    }
}
