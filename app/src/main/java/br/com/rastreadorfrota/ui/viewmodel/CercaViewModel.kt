package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.sync.SyncTrigger
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.ui.components.Avisos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Cercas (geofencing) e histórico de alertas. Mesmo padrão do resto: Room primeiro, nuvem depois. */
class CercaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Gravações no escopo do app: a tela pode fechar logo depois (salvar e voltar, "Desfazer").
    private val escopoApp = getApplication<RastreadorApp>().escopo
    private val cercaDao = db.cercaDao()
    private val alertaDao = db.alertaDao()

    val cercas: StateFlow<List<CercaEntity>> = cercaDao.observarTodas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val alertas: StateFlow<List<AlertaEntity>?> = alertaDao.observarRecentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    val veiculos: StateFlow<List<VeiculoEntity>> = db.veiculoDao().observarTodos()
        .map { lista -> lista.filter { it.operacional } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    suspend fun carregar(id: Long): CercaEntity? = cercaDao.buscarPorId(id)

    fun salvar(cerca: CercaEntity) {
        escopoApp.launch {
            val agora = System.currentTimeMillis()
            if (cerca.id == 0L) cercaDao.inserir(cerca.copy(sincronizado = false, updatedAt = agora))
            else cercaDao.atualizar(cerca.copy(sincronizado = false, updatedAt = agora))
            SyncTrigger.requestSync()
            Avisos.mostrar("Cerca \"${cerca.nome}\" salva. Os alertas começam agora.")
        }
    }

    fun alternarAtiva(cerca: CercaEntity) {
        escopoApp.launch {
            cercaDao.atualizar(cerca.copy(ativa = !cerca.ativa, sincronizado = false, updatedAt = System.currentTimeMillis()))
            SyncTrigger.requestSync()
        }
    }

    fun remover(cerca: CercaEntity) {
        escopoApp.launch {
            cercaDao.atualizar(cerca.copy(deletedLocally = true, sincronizado = false, updatedAt = System.currentTimeMillis()))
            SyncTrigger.requestSync()
            Avisos.mostrar("Cerca \"${cerca.nome}\" excluída.", "Desfazer") {
                escopoApp.launch {
                    cercaDao.atualizar(cerca.copy(deletedLocally = false, sincronizado = false, updatedAt = System.currentTimeMillis()))
                    SyncTrigger.requestSync()
                }
            }
        }
    }

    /** Atalho do primeiro uso: uma cerca de 200 m no centro de distribuição. */
    fun criarCercaDoCd() = salvar(
        CercaEntity(nome = NOME_CD, latitude = CENTRO_DISTRIBUICAO.latitude, longitude = CENTRO_DISTRIBUICAO.longitude, raioM = 200.0)
    )

    fun marcarTodosLidos() {
        escopoApp.launch { alertaDao.marcarTodosLidos() }
    }
}
