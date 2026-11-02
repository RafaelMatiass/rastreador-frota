package br.com.rastreadorfrota

import android.app.Application
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.geofence.NotificacaoAlertas
import br.com.rastreadorfrota.ui.theme.PreferenciaTema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.osmdroid.config.Configuration

/**
 * Ponto de partida do processo. Guarda o [escopo] que vive enquanto o app vive:
 * sincronização e monitoramento de cercas rodam nele, e não presos a uma tela.
 */
class RastreadorApp : Application() {

    val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // A política de uso dos servidores de tiles do OpenStreetMap pede um User-Agent do app.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@RastreadorApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
        PreferenciaTema.carregar(this)
        NotificacaoAlertas.criarCanal(this)
        SyncEngine.inicializar(this, escopo)
    }
}
