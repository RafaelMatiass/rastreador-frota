package br.com.rastreadorfrota.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.repository.VeiculoNoMapa
import br.com.rastreadorfrota.rota.formatarDistancia
import br.com.rastreadorfrota.rota.formatarDuracao
import br.com.rastreadorfrota.simulacao.EtapaViagem
import br.com.rastreadorfrota.simulacao.SituacaoViagem
import br.com.rastreadorfrota.simulacao.StatusVeiculo
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

@Composable
fun corDoStatus(status: StatusVeiculo): Color = when (status) {
    StatusVeiculo.EM_TRANSITO -> TrakSyncTheme.colors.success
    StatusVeiculo.PARADO -> TrakSyncTheme.colors.warning
    StatusVeiculo.SEM_MOTORISTA -> TrakSyncTheme.colors.textSecondary
}

val StatusVeiculo.icone: ImageVector
    get() = when (this) {
        StatusVeiculo.EM_TRANSITO -> Icons.Filled.NearMe
        StatusVeiculo.PARADO -> Icons.Filled.LocalParking
        StatusVeiculo.SEM_MOTORISTA -> Icons.Filled.PersonOff
    }

@Composable
fun StatusPill(status: StatusVeiculo) = StatusPill(status.label, corDoStatus(status), status.icone)

@Composable
fun corDaSituacao(situacao: SituacaoVeiculo): Color = when (situacao) {
    SituacaoVeiculo.PENDENTE -> TrakSyncTheme.colors.warning
    SituacaoVeiculo.ATIVO -> TrakSyncTheme.colors.success
    SituacaoVeiculo.INATIVO -> TrakSyncTheme.colors.textSecondary
}

val SituacaoVeiculo.icone: ImageVector
    get() = when (this) {
        SituacaoVeiculo.PENDENTE -> Icons.Filled.HourglassTop
        SituacaoVeiculo.ATIVO -> Icons.Filled.CheckCircle
        SituacaoVeiculo.INATIVO -> Icons.Filled.Block
    }

@Composable
fun StatusPill(situacao: SituacaoVeiculo) = StatusPill(situacao.label, corDaSituacao(situacao), situacao.icone)

/** "Indo ao Supermercado Central · faltam 2,3 km · 4 min", "Entregando no ... · sai em 1 min"... */
fun textoViagem(v: SituacaoViagem, curto: Boolean = false): String {
    val tempo = formatarDuracao(v.segundosRestantes.toDouble())
    val distancia = if (curto) "" else " · faltam ${formatarDistancia(v.metrosRestantes)}"
    return when (v.etapa) {
        EtapaViagem.IDA -> "A caminho de ${v.clienteNome}$distancia · $tempo"
        EtapaViagem.ENTREGA -> "Entregando em ${v.clienteNome} · sai em $tempo"
        EtapaViagem.VOLTA -> "Voltando ao CD$distancia · $tempo"
        EtapaViagem.DESCARGA -> "Descarregando no CD · sai em $tempo"
        EtapaViagem.FORA_DE_OPERACAO -> "Sem motorista · parado no CD"
    }
}

/** Para o TalkBack ler o card inteiro de uma vez: "ABC1D23, Em trânsito, 45 km/h, A caminho de...". */
fun descricaoAcessivel(item: VeiculoNoMapa): String =
    "${item.veiculo.placa}, ${item.veiculo.modelo}, ${item.status.label}, ${item.telemetria.velocidadeKmh} quilômetros por hora, " +
        textoViagem(item.viagem, curto = true)

/** Etapas mostradas no stepper da viagem. */
val ETAPAS_VIAGEM = listOf(EtapaViagem.IDA, EtapaViagem.ENTREGA, EtapaViagem.VOLTA, EtapaViagem.DESCARGA)

val EtapaViagem.rotuloCurto: String
    get() = when (this) {
        EtapaViagem.IDA -> "Ida"
        EtapaViagem.ENTREGA -> "Entrega"
        EtapaViagem.VOLTA -> "Volta"
        EtapaViagem.DESCARGA -> "Descarga"
        EtapaViagem.FORA_DE_OPERACAO -> "No pátio"
    }
