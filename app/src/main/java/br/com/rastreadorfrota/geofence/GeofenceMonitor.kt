package br.com.rastreadorfrota.geofence

import android.app.Application
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.OrigemAlerta
import br.com.rastreadorfrota.data.repository.FrotaAoVivoRepository
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val HISTORICO_MS = 30L * 24 * 60 * 60 * 1000

/**
 * Geofencing da frota SIMULADA, no aparelho do gestor (Entrega 8).
 *
 * Roda no escopo do app (não de uma tela): a cada passo da simulação confere as cercas e,
 * em cada entrada/saída, grava o alerta no Room e mostra a notificação, mesmo com o app em
 * segundo plano enquanto o processo estiver vivo.
 */
object GeofenceMonitor {

    private var job: Job? = null

    fun iniciar(app: Application, escopo: CoroutineScope) {
        job?.cancel()
        val db = AppDatabase.getInstance(app)
        val alertaDao = db.alertaDao()
        val frota = FrotaAoVivoRepository.getInstance(app).frota(modoMotorista = false)
        job = escopo.launch {
            alertaDao.removerAntigos(System.currentTimeMillis() - HISTORICO_MS)
            var estado: Map<String, Boolean>? = null
            combine(frota, db.cercaDao().observarTodas()) { veiculos, cercas -> veiculos to cercas }
                .collect { (veiculos, cercas) ->
                    val posicoes = veiculos.map {
                        PosicaoVeiculo(it.veiculo.placa, it.veiculo.tipo, it.telemetria.latitude, it.telemetria.longitude)
                    }
                    val (novoEstado, transicoes) = AvaliadorCercas.avaliar(estado, posicoes, cercas)
                    estado = novoEstado
                    // Instante alinhado ao passo: o mesmo evento tem o mesmo id em qualquer aparelho.
                    val agora = System.currentTimeMillis()
                    val instante = agora - agora % SimuladorFrota.INTERVALO_MS
                    transicoes.forEach { t ->
                        val motorista = veiculos.find { it.veiculo.placa == t.placa }?.motorista
                        val alerta = AlertaEntity(
                            id = "SIM-${t.cerca.remoteId ?: t.cerca.id}-${t.placa}-${t.tipo.name}-$instante",
                            cercaNome = t.cerca.nome,
                            placa = t.placa,
                            tipo = t.tipo.name,
                            origem = OrigemAlerta.SIMULADO.name,
                            instante = instante,
                            motoristaNome = motorista?.nome,
                            sincronizado = true
                        )
                        if (alertaDao.inserir(alerta) != -1L) NotificacaoAlertas.notificar(app, alerta)
                    }
                }
        }
    }

    fun parar() {
        job?.cancel()
        job = null
    }
}
