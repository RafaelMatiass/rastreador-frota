package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.MotoristaRepository
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.PontoTelemetria
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import br.com.rastreadorfrota.simulacao.SituacaoViagem
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn

data class VeiculoNoMapa(val veiculo: VeiculoEntity, val viagem: SituacaoViagem) {
    val telemetria: PontoTelemetria get() = viagem.telemetria
    val emOperacao: Boolean get() = viagem.etapa != EtapaViagem.FORA_DE_OPERACAO

    /** Em trânsito / parado pela telemetria; sem motorista ativo, "Sem motorista". */
    val status: StatusVeiculo get() = if (emOperacao) telemetria.status else StatusVeiculo.SEM_MOTORISTA
}

/**
 * Junta os veículos do cache local (Room) com a viagem simulada (posição, telemetria, destino).
 * Só faz viagem o veículo com motorista ATIVO associado; os demais ficam parados no CD.
 */
class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val veiculoRepository = VeiculoRepository(db.veiculoDao())
    private val motoristaRepository = MotoristaRepository(db.motoristaDao())

    /** null até a tela dizer quem está olhando o mapa (evita mostrar a regra errada no 1º quadro). */
    private val modoMotorista = MutableStateFlow<Boolean?>(null)

    fun definirModo(motorista: Boolean) {
        modoMotorista.value = motorista
    }

    /** null enquanto o Room ainda não respondeu (pra não piscar o "nenhum veículo"). */
    val frota: StateFlow<List<VeiculoNoMapa>?> =
        combine(
            veiculoRepository.veiculos,
            motoristaRepository.motoristas,
            SimuladorFrota.passos,
            modoMotorista.filterNotNull()
        ) { veiculos, motoristas, passo, motorista ->
            val comMotoristaAtivo = motoristas.filter { it.ativo }.mapNotNull { it.veiculoId }.toSet()
            veiculos
                .filter { it.ativo }
                .map { veiculo ->
                    // No modo motorista o veículo é o do próprio usuário logado, que é ativo
                    // por definição (inativo não entra no app). Além disso, o aparelho do
                    // motorista não baixa a tabela de motoristas.
                    val emOperacao = motorista || veiculo.id in comMotoristaAtivo
                    VeiculoNoMapa(
                        veiculo,
                        if (emOperacao) SimuladorFrota.situacao(veiculo.placa, passo)
                        else SimuladorFrota.foraDeOperacao(veiculo.placa)
                    )
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
