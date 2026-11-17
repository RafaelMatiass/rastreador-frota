package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.geocoding.GeocodingService
import br.com.rastreadorfrota.rota.Rota
import br.com.rastreadorfrota.rota.RotaService
import br.com.rastreadorfrota.data.local.entity.SituacaoEntrega
import br.com.rastreadorfrota.simulacao.CENTRO_DISTRIBUICAO
import br.com.rastreadorfrota.simulacao.Encadeamento
import br.com.rastreadorfrota.simulacao.NOME_CD
import br.com.rastreadorfrota.simulacao.encadearEntrega
import br.com.rastreadorfrota.simulacao.viagemDaEntrega
import br.com.rastreadorfrota.simulacao.LIMITE_DESVIO_M
import br.com.rastreadorfrota.simulacao.PolylineCodec
import br.com.rastreadorfrota.simulacao.SimuladorFrota
import br.com.rastreadorfrota.simulacao.distanciaAoTrajeto
import br.com.rastreadorfrota.simulacao.montarViagem
import br.com.rastreadorfrota.simulacao.pontoDeDesvio
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed interface EstadoNovaEntrega {
    data object Parado : EstadoNovaEntrega
    data object Gerando : EstadoNovaEntrega
    data class Erro(val mensagem: String) : EstadoNovaEntrega
    data class Criada(val id: Long) : EstadoNovaEntrega
}

/**
 * Entregas (gestor): atribuir ao veículo, cancelar e consultar o histórico.
 *
 * Ao atribuir, o app pede ao OSRM a rota CD -> cliente e cliente -> CD UMA vez e guarda
 * na entrega. Por isso é preciso internet só neste momento; a viagem em si é simulada
 * offline em qualquer aparelho a partir do que foi salvo.
 */
class EntregaViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).entregaDao()

    private val _criacao = MutableStateFlow<EstadoNovaEntrega>(EstadoNovaEntrega.Parado)
    val criacao: StateFlow<EstadoNovaEntrega> = _criacao.asStateFlow()

    fun doVeiculo(placa: String): Flow<List<EntregaEntity>> = dao.observarDoVeiculo(placa)
    fun observar(id: Long): Flow<EntregaEntity?> = dao.observar(id)

    fun criar(
        veiculo: VeiculoEntity,
        motoristaNome: String?,
        clienteNome: String,
        destino: GeoPoint,
        enderecoInformado: String?,
        comDesvio: Boolean
    ) {
        if (_criacao.value == EstadoNovaEntrega.Gerando) return
        _criacao.value = EstadoNovaEntrega.Gerando
        viewModelScope.launch {
            try {
                val agora = System.currentTimeMillis()
                // Fila: a nova entrega continua de onde a última do veículo para (ou sai do CD).
                val ativas = dao.observarDoVeiculo(veiculo.placa).first().filter { it.ativa(agora) }
                val partida = encadearEntrega(ativas, agora)
                val ua = getApplication<Application>().packageName
                val ida = RotaService.calcular(partida.origem, destino, ua)
                val volta = RotaService.calcular(destino, CENTRO_DISTRIBUICAO, ua)
                val idaExecutada = if (comDesvio) gerarDesvio(ida.pontos, partida.origem, destino, ua) else ida.pontos
                val endereco = enderecoInformado?.takeIf { it.isNotBlank() }
                    ?: runCatching { GeocodingService.endereco(destino).resumo }.getOrNull()
                    ?: "%.5f, %.5f".format(destino.latitude, destino.longitude)

                val viagem = montarViagem(clienteNome, endereco, ida.pontos, idaExecutada, volta.pontos, kmh(ida), kmh(volta))
                // Começa num passo do relógio: todos os aparelhos contam a partir do mesmo instante.
                val inicio = partida.inicio
                val entrega = EntregaEntity(
                    placa = veiculo.placa,
                    clienteNome = clienteNome,
                    endereco = endereco,
                    latitude = destino.latitude,
                    longitude = destino.longitude,
                    rotaIda = PolylineCodec.codificar(ida.pontos),
                    rotaVolta = PolylineCodec.codificar(volta.pontos),
                    rotaIdaExecutada = PolylineCodec.codificar(idaExecutada),
                    kmhIda = kmh(ida),
                    kmhVolta = kmh(volta),
                    comDesvio = comDesvio && idaExecutada !== ida.pontos,
                    origemNome = partida.origemNome,
                    inicioEm = inicio,
                    fimPrevistoEm = inicio + viagem.duracaoMs,
                    motoristaNome = motoristaNome,
                    criadaPorUid = FirebaseAuth.getInstance().currentUser?.uid,
                    criadaEm = agora,
                    updatedAt = agora,
                    sincronizado = false
                )
                // A anterior da fila não volta mais ao CD: termina quando esta começa.
                val anterior = ativas.maxByOrNull { it.fimPrevistoEm }
                if (anterior != null && partida.fimDaAnterior != null) {
                    dao.atualizar(anterior.copy(fimPrevistoEm = partida.fimDaAnterior, updatedAt = agora, sincronizado = false))
                }
                val id = dao.inserir(entrega)
                SyncEngine.sincronizarAgora()
                _criacao.value = EstadoNovaEntrega.Criada(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _criacao.value = EstadoNovaEntrega.Erro(
                    when (e) {
                        is UnknownHostException, is SocketTimeoutException -> "Sem internet para gerar a rota. A rota é calculada uma vez, ao atribuir."
                        else -> e.message ?: "Não foi possível gerar a rota da entrega."
                    }
                )
            }
        }
    }

    fun limparErro() {
        if (_criacao.value is EstadoNovaEntrega.Erro) _criacao.value = EstadoNovaEntrega.Parado
    }

    private val _avisoFila = MutableStateFlow<String?>(null)
    /** Erro ao replanejar a fila no cancelamento (ex.: sem internet para gerar a rota nova). */
    val avisoFila: StateFlow<String?> = _avisoFila.asStateFlow()

    /**
     * Cancela só esta entrega; as próximas da fila continuam e são replanejadas:
     * - cancelou a que estava em andamento: a próxima sai de onde o veículo está agora;
     * - cancelou uma do meio da fila: a seguinte sai do cliente da anterior a ela;
     * - cancelou a última: a anterior volta a ter a volta ao CD.
     * Quando a origem de uma entrega muda, a rota dela é gerada de novo no OSRM. Tudo é
     * calculado antes de gravar: se faltar internet, nada muda e o gestor é avisado.
     */
    fun cancelar(entrega: EntregaEntity) {
        viewModelScope.launch {
            _avisoFila.value = null
            try {
                val agora = System.currentTimeMillis()
                val proximoPasso = (agora / SimuladorFrota.INTERVALO_MS + 1) * SimuladorFrota.INTERVALO_MS
                val fila = dao.observarDoVeiculo(entrega.placa).first()
                    .filter { it.id != entrega.id && it.ativa(agora) }
                    .sortedBy { it.inicioEm }
                var anterior = fila.firstOrNull { it.situacao(agora) == SituacaoEntrega.EM_ANDAMENTO }
                val naFila = fila.filter { it.situacao(agora) == SituacaoEntrega.AGENDADA }
                // Cancelou a que estava rodando: quem vem depois parte da posição atual do veículo.
                val partidaInicial = if (anterior == null && entrega.situacao(agora) == SituacaoEntrega.EM_ANDAMENTO) {
                    val viagem = viagemDaEntrega(entrega)
                    val i = SimuladorFrota.indiceNaViagem(entrega, agora / SimuladorFrota.INTERVALO_MS).toInt().coerceIn(0, viagem.passos.lastIndex)
                    val t = viagem.passos[i].telemetria
                    Encadeamento(proximoPasso, GeoPoint(t.latitude, t.longitude), "posição de ${entrega.placa} (entrega cancelada)", null)
                } else null

                val ua = getApplication<Application>().packageName
                val alteradas = linkedMapOf<Long, EntregaEntity>()
                for (n in naFila) {
                    val partida = anterior?.let { encadearEntrega(listOf(it), agora) }
                        ?: partidaInicial
                        ?: Encadeamento(proximoPasso, CENTRO_DISTRIBUICAO, NOME_CD, null)
                    anterior?.let { if (it.fimPrevistoEm != partida.inicio) alteradas[it.id] = it.copy(fimPrevistoEm = partida.inicio) }
                    var nova = n.copy(inicioEm = partida.inicio, origemNome = partida.origemNome)
                    val idaAtual = PolylineCodec.decodificar(n.rotaIda)
                    if (idaAtual.isEmpty() || idaAtual.first().distanceToAsDouble(partida.origem) > 30) {
                        val destino = GeoPoint(n.latitude, n.longitude)
                        val ida = RotaService.calcular(partida.origem, destino, ua)
                        val executada = if (n.comDesvio) gerarDesvio(ida.pontos, partida.origem, destino, ua) else ida.pontos
                        nova = nova.copy(
                            rotaIda = PolylineCodec.codificar(ida.pontos),
                            rotaIdaExecutada = PolylineCodec.codificar(executada),
                            kmhIda = kmh(ida)
                        )
                    }
                    nova = nova.copy(fimPrevistoEm = nova.inicioEm + viagemDaEntrega(nova).duracaoMs)
                    alteradas[nova.id] = nova
                    anterior = nova
                }
                // A última da fila (a que sobrou) volta ao CD normalmente.
                anterior?.let { ultima ->
                    val atual = alteradas[ultima.id] ?: ultima
                    val completo = atual.inicioEm + viagemDaEntrega(atual).duracaoMs
                    if (atual.fimPrevistoEm != completo) alteradas[ultima.id] = atual.copy(fimPrevistoEm = completo)
                }

                dao.atualizar(entrega.copy(canceladaEm = agora, updatedAt = agora, sincronizado = false))
                alteradas.values.forEach { dao.atualizar(it.copy(updatedAt = agora, sincronizado = false)) }
                SyncEngine.sincronizarAgora()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _avisoFila.value = when (e) {
                    is UnknownHostException, is SocketTimeoutException ->
                        "Sem internet para replanejar a fila. A entrega não foi cancelada."
                    else -> "Não foi possível replanejar a fila: ${e.message}"
                }
            }
        }
    }

    /**
     * Desvio para demonstração: um ponto a 45% da ida, afastado para o lado, vira parada
     * intermediária no OSRM. Tenta os dois lados e distâncias maiores até a rota realmente
     * sair do caminho planejado; se não conseguir, a ida segue sem desvio.
     */
    private suspend fun gerarDesvio(planejada: List<GeoPoint>, origem: GeoPoint, destino: GeoPoint, ua: String): List<GeoPoint> {
        for (afastamento in listOf(400.0, -400.0, 700.0, -700.0)) {
            val via = pontoDeDesvio(planejada, 0.45, afastamento)
            val rota = runCatching { RotaService.calcularPor(listOf(origem, via, destino), ua).pontos }.getOrNull() ?: continue
            if (rota.maxOf { distanciaAoTrajeto(it, planejada) } > LIMITE_DESVIO_M * 2) return rota
        }
        return planejada
    }

    /** Velocidade média que o OSRM estimou para o trajeto (limitada a valores plausíveis na cidade). */
    private fun kmh(rota: Rota): Int =
        if (rota.duracaoSegundos <= 0) 40 else (rota.distanciaMetros / rota.duracaoSegundos * 3.6).toInt().coerceIn(20, 80)
}
