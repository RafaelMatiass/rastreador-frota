package br.com.rastreadorfrota.geofence

import android.app.Application
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.OrigemAlerta
import br.com.rastreadorfrota.data.local.entity.TipoAlerta
import br.com.rastreadorfrota.data.repository.FrotaAoVivoRepository
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val HISTORICO_MS = 30L * 24 * 60 * 60 * 1000

/**
 * Geofencing e desvio de rota da frota SIMULADA, no aparelho do gestor (Entrega 8).
 *
 * Roda no escopo do app (não de uma tela): a cada passo da simulação confere as cercas e a
 * distância de cada veículo em entrega até a rota planejada. Em cada entrada/saída de cerca
 * e em cada saída/volta à rota, grava o alerta no Room e mostra a notificação, mesmo com o
 * app em segundo plano enquanto o processo estiver vivo.
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
            // Desvio: por entrega, se o veículo estava fora da rota no passo anterior.
            var foraDaRota: Map<Long, Boolean>? = null
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

                    // Desvio de rota: só conta a mudança (a 1ª avaliação apenas registra o estado).
                    val emEntrega = veiculos.filter { it.entrega != null }
                    val novoForaDaRota = emEntrega.associate { it.entrega!!.id to it.foraDaRota }
                    emEntrega.forEach { item ->
                        val entrega = item.entrega!!
                        val antes = foraDaRota?.get(entrega.id) ?: return@forEach
                        if (antes == item.foraDaRota) return@forEach
                        val tipo = if (item.foraDaRota) TipoAlerta.DESVIO else TipoAlerta.RETORNO_ROTA
                        val alerta = AlertaEntity(
                            id = "ROTA-${entrega.remoteId ?: entrega.id}-${tipo.name}-$instante",
                            cercaNome = "rota para ${entrega.clienteNome}",
                            placa = item.veiculo.placa,
                            tipo = tipo.name,
                            origem = OrigemAlerta.SIMULADO.name,
                            instante = instante,
                            motoristaNome = item.motorista?.nome,
                            sincronizado = true
                        )
                        if (alertaDao.inserir(alerta) != -1L) NotificacaoAlertas.notificar(app, alerta)
                    }
                    foraDaRota = novoForaDaRota
                }
        }
    }

    fun parar() {
        job?.cancel()
        job = null
    }
}
