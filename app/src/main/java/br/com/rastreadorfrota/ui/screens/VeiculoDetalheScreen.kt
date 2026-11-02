package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.RegrasFrota
import br.com.rastreadorfrota.ui.components.AvatarUsuario
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.DialogoConfirmacao
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.InfoChip
import br.com.rastreadorfrota.ui.components.LinhaDado
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.SecaoTitulo
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.StepperViagem
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.textoViagem
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Ficha do veículo: foto, situação, telemetria ao vivo, motorista e ações do gestor. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeiculoDetalheScreen(
    veiculoId: Long,
    usuario: Usuario?,
    onBack: () -> Unit,
    onEditar: (Long) -> Unit,
    onVerNoMapa: (String) -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val veiculo by remember(veiculoId) { viewModel.observar(veiculoId) }.collectAsStateWithLifecycle(initialValue = null)
    val motoristas by viewModel.motoristas.collectAsStateWithLifecycle()
    val frota by remember { viewModel.frotaAoVivo(modoMotorista = false) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val ehGestor = usuario?.perfil == Perfil.CONTROLADOR
    var menu by remember { mutableStateOf(false) }
    var confirmarExclusao by remember { mutableStateOf(false) }
    var escolherMotorista by remember { mutableStateOf(false) }

    val v = veiculo
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(v?.placa ?: "Veículo") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } },
                actions = {
                    if (v != null && (ehGestor || v.cadastradoPorUid == usuario?.uid)) {
                        IconButton(onClick = { onEditar(v.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar veículo") }
                    }
                    if (v != null && ehGestor) {
                        Box {
                            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Mais ações") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                if (v.situacaoEnum == SituacaoVeiculo.INATIVO) {
                                    DropdownMenuItem(
                                        text = { Text("Reativar") },
                                        leadingIcon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                                        onClick = { menu = false; viewModel.reativar(v) }
                                    )
                                } else if (v.situacaoEnum == SituacaoVeiculo.ATIVO) {
                                    DropdownMenuItem(
                                        text = { Text("Desativar") },
                                        leadingIcon = { Icon(Icons.Filled.Block, contentDescription = null) },
                                        onClick = { menu = false; viewModel.desativar(v) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menu = false; confirmarExclusao = true }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (v == null) {
            EstadoVazio(Icons.Filled.Map, "Veículo não encontrado", "Ele pode ter sido excluído em outro aparelho.", Modifier.padding(padding))
            return@Scaffold
        }
        val tipo = tipoVeiculo(v.tipo)
        val motorista = motoristas.find { it.veiculoId == v.id }
        val aoVivo = frota.find { it.veiculo.id == v.id }
        val cadastradoPor = v.cadastradoPorUid?.let { uid -> motoristas.find { it.uid == uid }?.nome ?: if (uid == usuario?.uid) "Você" else null }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Box(Modifier.fillMaxWidth().height(220.dp)) {
                FotoVeiculo(v.fotoLocalPath, tipo.icone, Modifier.fillMaxSize().padding(horizontal = Espaco.tela))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = Espaco.tela)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))))
                )
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = Espaco.tela + Espaco.md, vertical = Espaco.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlacaBadge(v.placa)
                    Spacer(Modifier.width(Espaco.sm))
                    StatusPill(v.situacaoEnum)
                }
            }

            Column(Modifier.padding(Espaco.tela), verticalArrangement = Arrangement.spacedBy(Espaco.lg)) {
                Column {
                    Text(v.modelo, style = MaterialTheme.typography.headlineSmall)
                    Text(tipo.label, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)
                }

                if (v.situacaoEnum == SituacaoVeiculo.PENDENTE && ehGestor) {
                    CardTrak(destaque = true, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Espaco.lg)) {
                            Text("Aguardando sua aprovação", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Cadastrado por ${cadastradoPor ?: "um motorista"}. Confira os dados e a foto antes de liberar para a operação.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(Modifier.padding(top = Espaco.md), horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                                BotaoSecundario("Recusar", Icons.Filled.Delete, { confirmarExclusao = true }, Modifier.weight(1f))
                                BotaoPrincipal("Aprovar", { viewModel.aprovar(v) }, Modifier.weight(1f), icone = Icons.Filled.CheckCircle)
                            }
                        }
                    }
                }

                if (aoVivo != null) {
                    SecaoTitulo("Agora")
                    CardTrak(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(Espaco.lg)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusPill(aoVivo.status)
                                Spacer(Modifier.weight(1f))
                                BotaoSecundario("Ver no mapa", Icons.Filled.Map, { onVerNoMapa(v.placa) })
                            }
                            if (aoVivo.emOperacao) {
                                Spacer(Modifier.height(Espaco.md))
                                StepperViagem(aoVivo.viagem.etapa)
                            }
                            Text(textoViagem(aoVivo.viagem), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Espaco.md))
                            Row(
                                Modifier.padding(top = Espaco.md).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
                            ) {
                                InfoChip(Icons.Filled.Speed, "${aoVivo.telemetria.velocidadeKmh} km/h")
                                InfoChip(Icons.Filled.PowerSettingsNew, if (aoVivo.telemetria.motorLigado) "Motor ligado" else "Motor desligado")
                                InfoChip(Icons.Filled.SensorDoor, if (aoVivo.telemetria.portasAbertas) "Portas abertas" else "Portas fechadas")
                            }
                        }
                    }
                }

                SecaoTitulo("Motorista")
                CardTrak(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                        if (motorista != null) {
                            AvatarUsuario(motorista.uid, motorista.nome, 44.dp)
                            Spacer(Modifier.width(Espaco.md))
                            Column(Modifier.weight(1f)) {
                                Text(motorista.nome, style = MaterialTheme.typography.titleSmall)
                                Text("CNH ${motorista.categoriaCnh} · ${motorista.telefone}", style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
                            }
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = TrakSyncTheme.colors.textSecondary)
                            Spacer(Modifier.width(Espaco.md))
                            Text(
                                if (v.operacional) "Sem motorista: o veículo fica parado no CD." else "Aprove o veículo para poder atribuir um motorista.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (ehGestor && v.operacional) {
                        BotaoSecundario(
                            if (motorista == null) "Atribuir motorista" else "Trocar motorista",
                            if (motorista == null) Icons.Filled.PersonAdd else Icons.Filled.SwapHoriz,
                            { escolherMotorista = true },
                            Modifier.fillMaxWidth().padding(start = Espaco.lg, end = Espaco.lg, bottom = Espaco.lg)
                        )
                    }
                }

                SecaoTitulo("Dados")
                CardTrak(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = Espaco.lg, vertical = Espaco.sm)) {
                        LinhaDado("Capacidade", v.capacidadeCargaKg?.let { "%,.0f kg".format(it).replace(",", ".") } ?: "—")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Quilometragem", v.odometroKm?.let { "%,d km".format(it).replace(",", ".") } ?: "—")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Última vistoria", v.ultimaVistoriaEm?.let { data(it) } ?: "Nenhuma")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Cadastrado por", cadastradoPor ?: "Gestor")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Sincronização", if (v.sincronizado) "Na nuvem" else "Pendente")
                        v.observacoes?.let {
                            HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                            Text("Observações", style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
                        }
                    }
                }
            }
        }

        if (confirmarExclusao) {
            DialogoConfirmacao(
                titulo = if (v.situacaoEnum == SituacaoVeiculo.PENDENTE) "Recusar ${v.placa}?" else "Excluir ${v.placa}?",
                mensagem = "O veículo sai da frota em todos os aparelhos" + (motorista?.let { " e ${it.nome.substringBefore(" ")} fica sem veículo" } ?: "") + ".",
                confirmar = if (v.situacaoEnum == SituacaoVeiculo.PENDENTE) "Recusar" else "Excluir",
                destrutivo = true,
                icone = Icons.Filled.Delete,
                onConfirmar = {
                    confirmarExclusao = false
                    viewModel.remover(v)
                    onBack()
                },
                onCancelar = { confirmarExclusao = false }
            )
        }

        if (escolherMotorista) {
            SeletorMotorista(
                veiculo = v,
                motoristas = motoristas.filter { it.ativo },
                atual = motorista,
                onEscolher = { escolhido ->
                    escolherMotorista = false
                    viewModel.atribuirMotorista(v, escolhido)
                },
                onFechar = { escolherMotorista = false }
            )
        }
    }
}

/**
 * Lista de motoristas para o veículo. Quem já dirige outro veículo aparece com a placa:
 * ao escolher, ele passa para este (um motorista, um veículo).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeletorMotorista(
    veiculo: VeiculoEntity,
    motoristas: List<MotoristaEntity>,
    atual: MotoristaEntity?,
    onEscolher: (MotoristaEntity?) -> Unit,
    onFechar: () -> Unit
) {
    var trocar by remember { mutableStateOf<MotoristaEntity?>(null) }
    ModalBottomSheet(onDismissRequest = onFechar, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(bottom = Espaco.xl)) {
            Text("Motorista de ${veiculo.placa}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = Espaco.tela))
            Text(
                "Cada veículo tem um motorista. Escolher quem já está com outro veículo faz a troca.",
                style = MaterialTheme.typography.bodySmall,
                color = TrakSyncTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = Espaco.tela, vertical = Espaco.sm)
            )
            OpcaoMotorista("Nenhum (deixar sem motorista)", null, atual == null) { onEscolher(null) }
            motoristas.forEach { m ->
                val outroVeiculo = m.veiculoId != null && m.veiculoId != veiculo.id
                OpcaoMotorista(
                    m.nome,
                    if (outroVeiculo) "Hoje com outro veículo" else if (m.veiculoId == veiculo.id) "Atual" else "Livre",
                    m.uid == atual?.uid
                ) {
                    when {
                        m.uid == atual?.uid -> onFechar()
                        outroVeiculo || atual != null -> trocar = m
                        else -> onEscolher(m)
                    }
                }
            }
            if (motoristas.isEmpty()) {
                Text(
                    "Nenhum motorista ativo. Eles aparecem aqui depois de criar a conta no app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrakSyncTheme.colors.textSecondary,
                    modifier = Modifier.padding(Espaco.tela)
                )
            }
        }
    }
    trocar?.let { m ->
        val validade = RegrasFrota.diasParaVencerCnh(m.validadeCnh)
        DialogoConfirmacao(
            titulo = "Confirmar troca?",
            mensagem = buildString {
                append("${m.nome.substringBefore(" ")} passa a dirigir ${veiculo.placa}.")
                if (m.veiculoId != null && m.veiculoId != veiculo.id) append(" O veículo que estava com ${m.nome.substringBefore(" ")} fica livre.")
                if (atual != null) append(" ${atual.nome.substringBefore(" ")} fica sem veículo.")
                if (validade != null && validade < 0) append(" Atenção: a CNH dele(a) está vencida.")
            },
            confirmar = "Trocar",
            icone = Icons.Filled.SwapHoriz,
            onConfirmar = { trocar = null; onEscolher(m) },
            onCancelar = { trocar = null }
        )
    }
}

@Composable
private fun OpcaoMotorista(nome: String, detalhe: String?, selecionado: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Espaco.tela, vertical = Espaco.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (selecionado) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selecionado) MaterialTheme.colorScheme.primary else TrakSyncTheme.colors.textSecondary
        )
        Spacer(Modifier.width(Espaco.md))
        Column(Modifier.weight(1f)) {
            Text(nome, style = MaterialTheme.typography.bodyLarge)
            if (detalhe != null) Text(detalhe, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
        }
    }
}

private fun data(instante: Long) = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR")).format(Date(instante))

