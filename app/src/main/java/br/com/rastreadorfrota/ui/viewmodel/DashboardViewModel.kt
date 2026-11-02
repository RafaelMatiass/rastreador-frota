package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.FrotaAoVivoRepository
import br.com.rastreadorfrota.data.repository.RegrasFrota
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class TipoAtencao { PENDENTE, CNH_VENCENDO, PORTA_ABERTA, VELOCIDADE, SEM_MOTORISTA, MOTORISTA_SEM_VEICULO }

/** Um item da lista "Precisa de atenção" do dashboard. */
data class ItemAtencao(
    val tipo: TipoAtencao,
    val titulo: String,
    val detalhe: String,
    val veiculo: VeiculoEntity? = null,
    val placa: String? = null
)

data class EstadoDashboard(
    val frota: List<VeiculoNoMapa>,
    val totalVeiculos: Int,
    val motoristasAtivos: Int,
    val alertasHoje: Int,
    val alertasRecentes: List<AlertaEntity>,
    val atencao: List<ItemAtencao>
) {
    fun contar(status: StatusVeiculo) = frota.count { it.status == status }
    fun contar(etapa: EtapaViagem) = frota.count { it.viagem.etapa == etapa }

    /** Média só de quem está andando (parado puxaria a média para baixo sem dizer nada). */
    val velocidadeMedia: Int
        get() = frota.filter { it.status == StatusVeiculo.EM_TRANSITO }.map { it.telemetria.velocidadeKmh }
            .takeIf { it.isNotEmpty() }?.average()?.toInt() ?: 0

    /** Quem está indo entregar, do que chega antes ao que chega depois. */
    val proximasChegadas: List<VeiculoNoMapa>
        get() = frota.filter { it.viagem.etapa == EtapaViagem.IDA }.sortedBy { it.viagem.segundosRestantes }
}

/** Dashboard do gestor: tudo calculado das mesmas fontes do mapa (Room + simulação) e dos alertas. */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    private val inicioDoDia = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val estado: StateFlow<EstadoDashboard?> = combine(
        FrotaAoVivoRepository.getInstance(application).frota(modoMotorista = false),
        db.veiculoDao().observarTodos(),
        db.motoristaDao().observarTodos(),
        db.alertaDao().observarRecentes(5),
        db.alertaDao().observarQuantidadeDesde(inicioDoDia)
    ) { frota, veiculos, motoristas, recentes, alertasHoje ->
        EstadoDashboard(
            frota = frota,
            totalVeiculos = veiculos.count { it.situacaoEnum != SituacaoVeiculo.INATIVO },
            motoristasAtivos = motoristas.count { it.ativo },
            alertasHoje = alertasHoje,
            alertasRecentes = recentes,
            atencao = itensDeAtencao(frota, veiculos, motoristas)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    private fun itensDeAtencao(
        frota: List<VeiculoNoMapa>,
        veiculos: List<VeiculoEntity>,
        motoristas: List<MotoristaEntity>
    ): List<ItemAtencao> = buildList {
        val nomePorUid = motoristas.associate { it.uid to it.nome.substringBefore(" ") }
        veiculos.filter { it.situacaoEnum == SituacaoVeiculo.PENDENTE }.forEach {
            add(
                ItemAtencao(
                    TipoAtencao.PENDENTE,
                    "${it.placa} aguarda aprovação",
                    "${it.modelo} · cadastrado por ${it.cadastradoPorUid?.let(nomePorUid::get) ?: "um motorista"}",
                    veiculo = it
                )
            )
        }
        frota.filter { it.portaAbertaEmMovimento }.forEach {
            add(ItemAtencao(TipoAtencao.PORTA_ABERTA, "${it.veiculo.placa} com porta aberta em movimento", "${it.telemetria.velocidadeKmh} km/h", placa = it.veiculo.placa))
        }
        frota.filter { it.acimaDaVelocidade }.forEach {
            add(
                ItemAtencao(
                    TipoAtencao.VELOCIDADE,
                    "${it.veiculo.placa} acima de ${VeiculoNoMapa.LIMITE_VELOCIDADE_KMH} km/h",
                    "${it.telemetria.velocidadeKmh} km/h · ${it.motorista?.nome?.substringBefore(" ") ?: "sem motorista"}",
                    placa = it.veiculo.placa
                )
            )
        }
        motoristas.filter { it.ativo }.forEach { m ->
            val dias = RegrasFrota.diasParaVencerCnh(m.validadeCnh) ?: return@forEach
            if (dias <= 30) {
                add(
                    ItemAtencao(
                        TipoAtencao.CNH_VENCENDO,
                        if (dias < 0) "CNH de ${m.nome.substringBefore(" ")} vencida" else "CNH de ${m.nome.substringBefore(" ")} vence em $dias dia(s)",
                        "Validade ${m.validadeCnh}"
                    )
                )
            }
        }
        val semMotorista = frota.count { !it.emOperacao }
        if (semMotorista > 0) {
            add(ItemAtencao(TipoAtencao.SEM_MOTORISTA, "$semMotorista veículo(s) parado(s) sem motorista", "Atribua um motorista na aba Equipe"))
        }
        val semVeiculo = motoristas.count { it.ativo && it.veiculoId == null }
        if (semVeiculo > 0) {
            add(ItemAtencao(TipoAtencao.MOTORISTA_SEM_VEICULO, "$semVeiculo motorista(s) sem veículo", "Eles não aparecem no mapa até receber um veículo"))
        }
    }
}
