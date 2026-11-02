package br.com.rastreadorfrota

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rastreadorfrota.auth.EstadoSessao
import br.com.rastreadorfrota.auth.SessaoViewModel
import br.com.rastreadorfrota.geofence.NotificacaoAlertas
import br.com.rastreadorfrota.navigation.AppNavGraph
import br.com.rastreadorfrota.ui.theme.RastreadorFrotaTheme

class MainActivity : ComponentActivity() {

    private val sessao: SessaoViewModel by viewModels()

    /** Placa vinda do toque numa notificação de cerca. */
    private val placaNotificacao = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // A splash do sistema fica até sabermos se há alguém logado: sem piscar a tela de login.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { sessao.estado.value is EstadoSessao.Carregando }
        enableEdgeToEdge()
        placaNotificacao.value = intent?.getStringExtra(NotificacaoAlertas.EXTRA_PLACA)

        setContent {
            RastreadorFrotaTheme {
                AppNavGraph(
                    sessao = sessao,
                    placaNotificacao = placaNotificacao.value,
                    onPlacaConsumida = { placaNotificacao.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(NotificacaoAlertas.EXTRA_PLACA)?.let { placaNotificacao.value = it }
    }
}
