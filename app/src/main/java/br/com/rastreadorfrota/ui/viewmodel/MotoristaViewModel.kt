package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.MotoristaRepository
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.data.sync.SyncTrigger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MotoristaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = MotoristaRepository(db.motoristaDao())
    private val veiculoRepository = VeiculoRepository(db.veiculoDao())

    val motoristas: StateFlow<List<MotoristaEntity>> = repository.motoristas.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val veiculosDisponiveis: StateFlow<List<VeiculoEntity>> = veiculoRepository.veiculos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun salvarMotorista(nome: String, cnh: String, telefone: String, email: String, veiculoId: Long?) {
        viewModelScope.launch {
            repository.salvar(
                MotoristaEntity(
                    nome = nome.trim(),
                    cnh = cnh.trim(),
                    telefone = telefone.trim(),
                    email = email.trim(),
                    veiculoId = veiculoId
                )
            )
            SyncTrigger.requestSync()
        }
    }

    fun removerMotorista(motorista: MotoristaEntity) {
        viewModelScope.launch {
            repository.remover(motorista)
            SyncTrigger.requestSync()
        }
    }
}