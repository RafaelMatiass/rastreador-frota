package br.com.rastreadorfrota.auth

import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.geofence.GeofenceAparelho
import br.com.rastreadorfrota.geofence.GeofenceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * O que acontece no app inteiro quando alguém entra ou sai, independente de tela:
 * - sincronização automática liga/desliga;
 * - gestor: monitor de cercas da frota simulada;
 * - motorista: cercas reais registradas no GPS do aparelho.
 */
object SessaoApp {

    private val _usuario = MutableStateFlow<Usuario?>(null)

    /** Usuário logado agora (null = ninguém). Os monitores de cerca observam isso. */
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciar(app: RastreadorApp, usuario: Usuario) {
        val trocouDePerfil = _usuario.value?.perfil != usuario.perfil
        _usuario.value = usuario
        if (!trocouDePerfil) return
        SyncEngine.iniciarSessao(usuario.perfil)
        when (usuario.perfil) {
            Perfil.CONTROLADOR -> GeofenceMonitor.iniciar(app, app.escopo)
            Perfil.MOTORISTA -> GeofenceAparelho.iniciar(app, app.escopo)
        }
        SyncEngine.sincronizarAgora()
    }

    /** Dados do perfil mudaram (ex.: o gestor atribuiu um veículo): só atualiza quem observa. */
    fun atualizar(usuario: Usuario) {
        if (_usuario.value?.uid == usuario.uid) _usuario.value = usuario
    }

    /**
     * Saída: para tudo e limpa o cache local, para os dados de uma conta (ex.: motoristas da
     * empresa) não ficarem visíveis para a próxima pessoa que entrar neste aparelho.
     */
    suspend fun encerrar(app: RastreadorApp) {
        _usuario.value = null
        SyncEngine.encerrarSessao()
        GeofenceMonitor.parar()
        GeofenceAparelho.parar(app)
        withContext(Dispatchers.IO) { AppDatabase.getInstance(app).clearAllTables() }
    }
}
