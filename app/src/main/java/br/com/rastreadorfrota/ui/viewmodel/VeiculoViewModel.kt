package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.data.sync.SyncTrigger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VeiculoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VeiculoRepository(
        AppDatabase.getInstance(application).veiculoDao()
    )

    val veiculos: StateFlow<List<VeiculoEntity>> = repository.veiculos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun salvarVeiculo(
        placa: String,
        modelo: String,
        tipo: String,
        capacidadeCargaKg: Double?,
        fotoLocalPath: String?
    ) {
        viewModelScope.launch {
            repository.salvar(
                VeiculoEntity(
                    placa = placa.trim().uppercase(),
                    modelo = modelo.trim(),
                    tipo = tipo,
                    capacidadeCargaKg = capacidadeCargaKg,
                    fotoLocalPath = fotoLocalPath
                )
            )
            SyncTrigger.requestSync()
        }
    }

    fun removerVeiculo(veiculo: VeiculoEntity) {
        viewModelScope.launch {
            veiculo.fotoLocalPath?.let { File(it).delete() }
            repository.remover(veiculo)
            SyncTrigger.requestSync()
        }
    }
}