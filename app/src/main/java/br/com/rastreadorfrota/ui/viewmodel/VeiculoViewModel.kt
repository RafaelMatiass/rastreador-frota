package br.com.rastreadorfrota.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.AppDatabase
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.FrotaAoVivoRepository
import br.com.rastreadorfrota.data.repository.MotoristaRepository
import br.com.rastreadorfrota.data.repository.RegrasFrota
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.data.repository.VeiculoRepository
import br.com.rastreadorfrota.data.sync.SyncTrigger
import br.com.rastreadorfrota.ui.components.Avisos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

/** O que a pessoa digitou no formulário do veículo (tudo texto, validado ao salvar). */
data class FormVeiculo(
    val placa: String = "",
    val modelo: String = "",
    val tipo: String = "CAMINHAO",
    val capacidade: String = "",
    val odometro: String = "",
    val observacoes: String = "",
    val fotoLocalPath: String? = null
)

enum class CampoVeiculo { PLACA, MODELO, CAPACIDADE, ODOMETRO }

sealed interface ResultadoSalvar {
    data object Ok : ResultadoSalvar
    data class Erros(val campos: Map<CampoVeiculo, String>) : ResultadoSalvar
}

/**
 * Cadastro e gestão dos veículos, com as regras de cada perfil:
 * - motorista cadastra (fica PENDENTE) e faz a vistoria do veículo dele;
 * - gestor cadastra já ATIVO, aprova, desativa, exclui e atribui motorista.
 */
class VeiculoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    // Gravações no escopo do app: a tela pode fechar logo depois (salvar e voltar, "Desfazer").
    private val escopoApp = getApplication<RastreadorApp>().escopo
    private val repository = VeiculoRepository(db.veiculoDao())
    private val motoristaRepository = MotoristaRepository(db.motoristaDao())

    val veiculos: StateFlow<List<VeiculoEntity>> = repository.veiculos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val motoristas: StateFlow<List<MotoristaEntity>> = motoristaRepository.motoristas.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun observar(id: Long): Flow<VeiculoEntity?> = repository.observar(id)

    fun frotaAoVivo(modoMotorista: Boolean): Flow<List<VeiculoNoMapa>> =
        FrotaAoVivoRepository.getInstance(getApplication()).frota(modoMotorista)

    suspend fun carregar(id: Long): VeiculoEntity? = repository.buscar(id)

    suspend fun salvar(form: FormVeiculo, existente: VeiculoEntity?, usuario: Usuario): ResultadoSalvar {
        val placa = RegrasFrota.normalizarPlaca(form.placa)
        val erros = buildMap {
            RegrasFrota.validarPlaca(placa)?.let { put(CampoVeiculo.PLACA, it) }
            if (form.modelo.isBlank()) put(CampoVeiculo.MODELO, "Informe o modelo (ex.: Mercedes Accelo 1016).")
            if (form.capacidade.isNotBlank() && form.capacidade.replace(",", ".").toDoubleOrNull() == null) {
                put(CampoVeiculo.CAPACIDADE, "Use só números (ex.: 3500).")
            }
            if (form.odometro.isNotBlank() && form.odometro.filter { it.isDigit() }.toIntOrNull() == null) {
                put(CampoVeiculo.ODOMETRO, "Use só números.")
            }
        }.toMutableMap()
        if (CampoVeiculo.PLACA !in erros && repository.placaEmUso(placa, existente?.id ?: 0)) {
            erros[CampoVeiculo.PLACA] = "Já existe um veículo com essa placa na frota."
        }
        if (erros.isNotEmpty()) return ResultadoSalvar.Erros(erros)

        val ehGestor = usuario.perfil == Perfil.CONTROLADOR
        val capacidade = form.capacidade.replace(",", ".").toDoubleOrNull()
        val odometro = form.odometro.filter { it.isDigit() }.toIntOrNull()
        val observacoes = form.observacoes.trim().ifBlank { null }
        if (existente == null) {
            val situacao = if (ehGestor) SituacaoVeiculo.ATIVO else SituacaoVeiculo.PENDENTE
            repository.salvar(
                VeiculoEntity(
                    placa = placa,
                    modelo = form.modelo.trim(),
                    tipo = form.tipo,
                    capacidadeCargaKg = capacidade,
                    fotoLocalPath = form.fotoLocalPath,
                    situacao = situacao.name,
                    ativo = situacao == SituacaoVeiculo.ATIVO,
                    cadastradoPorUid = usuario.uid,
                    odometroKm = odometro,
                    observacoes = observacoes
                )
            )
            Avisos.mostrar(if (ehGestor) "Veículo $placa adicionado à frota." else "Veículo $placa enviado para aprovação do gestor.")
        } else {
            // Foto trocada: a antiga sai do aparelho.
            if (existente.fotoLocalPath != null && existente.fotoLocalPath != form.fotoLocalPath) {
                runCatching { File(existente.fotoLocalPath).delete() }
            }
            repository.atualizar(
                existente.copy(
                    placa = placa,
                    modelo = form.modelo.trim(),
                    tipo = form.tipo,
                    capacidadeCargaKg = capacidade,
                    fotoLocalPath = form.fotoLocalPath,
                    odometroKm = odometro,
                    observacoes = observacoes
                )
            )
            Avisos.mostrar("Dados de $placa atualizados.")
        }
        SyncTrigger.requestSync()
        return ResultadoSalvar.Ok
    }

    /** Vistoria do motorista: foto nova (opcional), km atual e observações. */
    fun registrarVistoria(veiculo: VeiculoEntity, fotoLocalPath: String?, odometro: Int?, observacoes: String) {
        escopoApp.launch {
            if (fotoLocalPath != null && veiculo.fotoLocalPath != null && veiculo.fotoLocalPath != fotoLocalPath) {
                runCatching { File(veiculo.fotoLocalPath).delete() }
            }
            repository.atualizar(
                veiculo.copy(
                    fotoLocalPath = fotoLocalPath ?: veiculo.fotoLocalPath,
                    odometroKm = odometro ?: veiculo.odometroKm,
                    observacoes = observacoes.trim().ifBlank { veiculo.observacoes },
                    ultimaVistoriaEm = System.currentTimeMillis()
                )
            )
            SyncTrigger.requestSync()
            Avisos.mostrar("Vistoria de ${veiculo.placa} registrada.")
        }
    }

    fun aprovar(veiculo: VeiculoEntity) = mudarSituacao(veiculo, SituacaoVeiculo.ATIVO, "${veiculo.placa} aprovado e liberado para a operação.")

    fun reativar(veiculo: VeiculoEntity) = mudarSituacao(veiculo, SituacaoVeiculo.ATIVO, "${veiculo.placa} voltou para a operação.")

    /** Desativado, o veículo sai da operação e fica sem motorista. */
    fun desativar(veiculo: VeiculoEntity) {
        escopoApp.launch {
            motoristaRepository.liberarVeiculo(veiculo.id)
            repository.alterarSituacao(veiculo, SituacaoVeiculo.INATIVO)
            SyncTrigger.requestSync()
            Avisos.mostrar("${veiculo.placa} desativado.", "Desfazer") { reativar(veiculo) }
        }
    }

    private fun mudarSituacao(veiculo: VeiculoEntity, situacao: SituacaoVeiculo, mensagem: String) {
        escopoApp.launch {
            repository.alterarSituacao(veiculo, situacao)
            SyncTrigger.requestSync()
            Avisos.mostrar(mensagem)
        }
    }

    /** Exclusão com "Desfazer": até a sincronização confirmar, é só uma marcação local. */
    fun remover(veiculo: VeiculoEntity) {
        escopoApp.launch {
            motoristaRepository.liberarVeiculo(veiculo.id)
            repository.remover(veiculo)
            SyncTrigger.requestSync()
            Avisos.mostrar("${veiculo.placa} excluído.", "Desfazer") {
                escopoApp.launch {
                    repository.desfazerRemocao(veiculo)
                    SyncTrigger.requestSync()
                }
            }
        }
    }

    /** Gestor atribui um motorista a este veículo (quem estava com ele fica sem). */
    fun atribuirMotorista(veiculo: VeiculoEntity, motorista: MotoristaEntity?) {
        escopoApp.launch {
            if (motorista == null) motoristaRepository.liberarVeiculo(veiculo.id)
            else motoristaRepository.associarVeiculo(motorista, veiculo.id)
            SyncTrigger.requestSync()
            Avisos.mostrar(motorista?.let { "${veiculo.placa} agora está com ${it.nome.substringBefore(" ")}." } ?: "${veiculo.placa} ficou sem motorista.")
        }
    }
}
