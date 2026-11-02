package br.com.rastreadorfrota.data.repository

import android.content.Context
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.PontoTelemetria
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import br.com.rastreadorfrota.simulacao.SituacaoViagem
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.osmdroid.util.GeoPoint

/** Um veículo da frota agora: cadastro, motorista (se houver) e onde está na viagem simulada. */
data class VeiculoNoMapa(
    val veiculo: VeiculoEntity,
    val viagem: SituacaoViagem,
    val motorista: MotoristaEntity? = null
) {
    val telemetria: PontoTelemetria get() = viagem.telemetria
    val emOperacao: Boolean get() = viagem.etapa != EtapaViagem.FORA_DE_OPERACAO
    val posicao: GeoPoint get() = GeoPoint(telemetria.latitude, telemetria.longitude)

    /** Em trânsito / parado pela telemetria; sem motorista ativo, "Sem motorista". */
    val status: StatusVeiculo get() = if (emOperacao) telemetria.status else StatusVeiculo.SEM_MOTORISTA

    /** Porta aberta com o veículo andando: anomalia que o gestor precisa ver. */
    val portaAbertaEmMovimento: Boolean get() = telemetria.portasAbertas && telemetria.velocidadeKmh > 0

    val acimaDaVelocidade: Boolean get() = telemetria.velocidadeKmh > LIMITE_VELOCIDADE_KMH

    companion object {
        const val LIMITE_VELOCIDADE_KMH = 60
    }
}

/**
 * Junta os veículos do cache local (Room) com a viagem simulada (posição, telemetria, destino).
 * É a mesma conta para o mapa, o dashboard, o geofencing e o Início do motorista.
 *
 * Só viaja o veículo APROVADO com motorista ATIVO associado; os demais ficam parados no CD.
 * Veículo pendente de aprovação ou inativo não aparece na operação.
 */
class FrotaAoVivoRepository private constructor(db: AppDatabase) {

    private val veiculos = db.veiculoDao().observarTodos()
    private val motoristas = db.motoristaDao().observarTodos()

    /**
     * [modoMotorista]: no aparelho do motorista não há tabela de motoristas (ele não baixa os
     * dados dos colegas), e o veículo dele viaja por definição (motorista inativo não entra no app).
     */
    fun frota(modoMotorista: Boolean): Flow<List<VeiculoNoMapa>> =
        combine(veiculos, motoristas, SimuladorFrota.passos) { veiculos, motoristas, passo ->
            val motoristaPorVeiculo = motoristas.filter { it.ativo && it.veiculoId != null }.associateBy { it.veiculoId }
            veiculos
                .filter { it.operacional }
                .map { veiculo ->
                    val motorista = motoristaPorVeiculo[veiculo.id]
                    val emOperacao = modoMotorista || motorista != null
                    VeiculoNoMapa(
                        veiculo = veiculo,
                        viagem = if (emOperacao) SimuladorFrota.situacao(veiculo.placa, passo)
                        else SimuladorFrota.foraDeOperacao(veiculo.placa),
                        motorista = motorista
                    )
                }
        }

    companion object {
        @Volatile
        private var INSTANCE: FrotaAoVivoRepository? = null

        fun getInstance(context: Context): FrotaAoVivoRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: FrotaAoVivoRepository(AppDatabase.getInstance(context)).also { INSTANCE = it }
            }
    }
}
