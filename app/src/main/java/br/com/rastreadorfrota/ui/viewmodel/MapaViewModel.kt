package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.simulacao.PontoTelemetria
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import br.com.rastreadorfrota.simulacao.SituacaoViagem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class VeiculoNoMapa(val veiculo: VeiculoEntity, val viagem: SituacaoViagem) {
    val telemetria: PontoTelemetria get() = viagem.telemetria
}

/** Junta os veículos do cache local (Room) com a viagem simulada (posição, telemetria, destino). */
class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VeiculoRepository(
        AppDatabase.getInstance(application).veiculoDao()
    )

    /** null enquanto o Room ainda não respondeu (pra não piscar o "nenhum veículo"). */
    val frota: StateFlow<List<VeiculoNoMapa>?> =
        combine(repository.veiculos, SimuladorFrota.passos) { veiculos, passo ->
            veiculos
                .filter { it.ativo }
                .map { VeiculoNoMapa(it, SimuladorFrota.situacao(it.placa, passo)) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
