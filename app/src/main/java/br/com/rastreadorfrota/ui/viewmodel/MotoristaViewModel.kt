package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.MotoristaRepository
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.data.sync.SyncTrigger
import br.com.rastreadorfrota.ui.components.Avisos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Equipe de motoristas (gestor): atribuir veículo (um por motorista) e ativar/desativar. */
class MotoristaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Gravações no escopo do app: a tela pode fechar logo depois (salvar e voltar, "Desfazer").
    private val escopoApp = getApplication<RastreadorApp>().escopo
    private val repository = MotoristaRepository(db.motoristaDao())
    private val veiculoRepository = VeiculoRepository(db.veiculoDao())

    val motoristas: StateFlow<List<MotoristaEntity>> = repository.motoristas.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Só veículo aprovado pode ser atribuído. */
    val veiculosDisponiveis: StateFlow<List<VeiculoEntity>> = veiculoRepository.veiculos
        .map { lista -> lista.filter { it.operacional } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun associarVeiculo(motorista: MotoristaEntity, veiculo: VeiculoEntity?) {
        escopoApp.launch {
            repository.associarVeiculo(motorista, veiculo?.id)
            SyncTrigger.requestSync()
            val nome = motorista.nome.substringBefore(" ")
            Avisos.mostrar(veiculo?.let { "$nome agora dirige ${it.placa}." } ?: "$nome ficou sem veículo.")
        }
    }

    fun alterarAtivo(motorista: MotoristaEntity, ativo: Boolean) {
        escopoApp.launch {
            repository.alterarAtivo(motorista, ativo)
            SyncTrigger.requestSync()
            val nome = motorista.nome.substringBefore(" ")
            if (ativo) Avisos.mostrar("$nome pode voltar a usar o app.")
            else Avisos.mostrar("$nome foi desativado.", "Desfazer") { alterarAtivo(motorista.copy(ativo = false), true) }
        }
    }
}
