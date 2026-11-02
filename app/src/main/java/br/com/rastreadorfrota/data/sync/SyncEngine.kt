package br.com.rastreadorfrota.data.sync

import android.app.Application
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.data.connectivity.ConnectivityObserver
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.geofence.NotificacaoAlertas
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import java.io.IOException

// Timeout de segurança: se o Firestore não responder nesse prazo (rede
// instável, regras bloqueando, projeto mal configurado), a sincronização é
// cancelada e reportada como erro em vez de girar pra sempre.
private const val TIMEOUT_SYNC_MS = 20_000L

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
    data class Success(val log: List<String>, val instante: Long = System.currentTimeMillis()) : SyncStatus
    data class Error(val mensagem: String) : SyncStatus
}

/**
 * Sincronização Room <-> Firestore no escopo do APP (uma só para o app inteiro).
 *
 * Antes, cada tela inicial criava o próprio SyncViewModel com um loop de 60 s; com as abas,
 * isso viraria vários loops em paralelo. Aqui existe um único motor, que só roda enquanto há
 * um usuário logado ([iniciarSessao] / [encerrarSessao]).
 */
@OptIn(FlowPreview::class)
object SyncEngine {

    private lateinit var app: Application
    private lateinit var escopo: CoroutineScope
    private lateinit var syncManager: FirestoreSyncManager

    lateinit var isOnline: StateFlow<Boolean>
        private set

    /** Quantas alterações locais ainda não subiram (veículos, motoristas, cercas, alertas). */
    lateinit var pendentes: StateFlow<Int>
        private set

    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var perfil: Perfil? = null
    private var sessao: Job? = null

    // Garante que só existe UMA sincronização em andamento por vez.
    private val mutex = Mutex()

    fun inicializar(application: Application, scope: CoroutineScope) {
        app = application
        escopo = scope
        val db = AppDatabase.getInstance(application)
        syncManager = FirestoreSyncManager(db.veiculoDao(), db.motoristaDao(), db.cercaDao(), db.alertaDao()).apply {
            // Alerta do GPS de um motorista chegou pela nuvem: o gestor é avisado.
            aoReceberAlerta = { NotificacaoAlertas.notificar(app, it) }
        }
        isOnline = ConnectivityObserver(application).observar()
            .stateIn(scope, SharingStarted.Eagerly, initialValue = false)
        pendentes = combine(
            db.veiculoDao().observarQuantidadePendentes(),
            db.motoristaDao().observarQuantidadePendentes(),
            db.cercaDao().observarQuantidadePendentes(),
            db.alertaDao().observarQuantidadePendentes()
        ) { v, m, c, a -> v + m + c + a }
            .stateIn(scope, SharingStarted.Eagerly, initialValue = 0)
    }

    /** Usuário entrou: liga os gatilhos de sincronização automática. */
    fun iniciarSessao(perfilUsuario: Perfil) {
        if (perfil == perfilUsuario && sessao?.isActive == true) return
        sessao?.cancel()
        perfil = perfilUsuario
        sessao = escopo.launch {
            // Gatilho 1: a conexão voltou → sincroniza.
            launch {
                var estavaOnline = false
                isOnline.collect { online ->
                    if (online && !estavaOnline) sincronizarAgora()
                    estavaOnline = online
                }
            }
            // Gatilho 2: algo foi salvo/removido em qualquer tela → sincroniza.
            launch {
                SyncTrigger.requests.debounce(TRIGGER_DEBOUNCE_MS).collect { sincronizarAgora() }
            }
            // Gatilho 3: rede de segurança — a cada 60s, se estiver online.
            launch {
                while (true) {
                    delay(AUTO_SYNC_INTERVAL_MS)
                    if (isOnline.value) sincronizarAgora()
                }
            }
        }
    }

    fun encerrarSessao() {
        sessao?.cancel()
        sessao = null
        perfil = null
        _status.value = SyncStatus.Idle
    }

    fun sincronizarAgora() {
        val perfilAtual = perfil ?: return
        // tryLock: se já tem sync rodando, essa chamada desiste sem criar condição de corrida.
        if (!mutex.tryLock()) return
        escopo.launch {
            _status.value = SyncStatus.Syncing
            try {
                val resultado = withTimeout(TIMEOUT_SYNC_MS) { syncManager.sincronizarTudo(perfilAtual) }
                _status.value = SyncStatus.Success(resultado.log)
            } catch (e: TimeoutCancellationException) {
                _status.value = SyncStatus.Error("Tempo esgotado. Verifique a conexão e tente de novo.")
            } catch (e: Exception) {
                _status.value = SyncStatus.Error(mensagemDeErro(e))
            } finally {
                mutex.unlock()
            }
        }
    }

    private fun mensagemDeErro(e: Exception): String = when {
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Sem permissão no Firestore. Publique as regras atualizadas (firestore.rules)."
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sem conexão com o servidor. As alterações ficam guardadas no aparelho."
        e is IOException -> "Sem conexão. As alterações ficam guardadas no aparelho."
        else -> "Não foi possível sincronizar agora. Tentaremos de novo em instantes."
    }
}
