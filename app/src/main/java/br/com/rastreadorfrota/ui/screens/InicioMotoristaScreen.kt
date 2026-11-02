package br.com.rastreadorfrota.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SensorDoor
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.RastreadorApp
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.OrigemAlerta
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.geocoding.Endereco
import br.com.rastreadorfrota.geocoding.GeocodingService
import br.com.rastreadorfrota.geofence.EstadoCercasAparelho
import br.com.rastreadorfrota.geofence.GeofenceAparelho
import br.com.rastreadorfrota.navigation.PedidoMapa
import br.com.rastreadorfrota.navigation.PedidosNavegacao
import br.com.rastreadorfrota.navigation.Rotas
import br.com.rastreadorfrota.rota.NavegacaoExterna
import br.com.rastreadorfrota.ui.components.AvisoPermissao
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.ChipSincronizacao
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.InfoChip
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.SecaoTitulo
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.StepperViagem
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.textoViagem
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.CercaViewModel
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel

/**
 * Início do motorista: o turno de hoje. Veículo, viagem atual com atalho para navegar,
 * telemetria e os alertas de chegada/saída pelo GPS do aparelho.
 */
@Composable
fun InicioMotoristaScreen(
    usuario: Usuario?,
    onAbrirPerfil: () -> Unit,
    onAbrirAba: (String) -> Unit,
    onCadastrarVeiculo: () -> Unit,
    veiculoViewModel: VeiculoViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    cercaViewModel: CercaViewModel = viewModel()
) {
    val context = LocalContext.current
    val veiculos by veiculoViewModel.veiculos.collectAsStateWithLifecycle()
    val frota by remember { veiculoViewModel.frotaAoVivo(modoMotorista = true) }.collectAsStateWithLifecycle(initialValue = null)
    val alertas by cercaViewModel.alertas.collectAsStateWithLifecycle()
    val isOnline by syncViewModel.isOnline.collectAsStateWithLifecycle()
    val status by syncViewModel.status.collectAsStateWithLifecycle()
    val pendentes by syncViewModel.pendentes.collectAsStateWithLifecycle()

    val meuVeiculo = usuario?.veiculoRemoteId?.let { id -> veiculos.find { it.remoteId == id } }
    val aoVivo = frota?.find { it.veiculo.id == meuVeiculo?.id }
    val propostas = veiculos.filter { it.cadastradoPorUid == usuario?.uid && it.situacaoEnum == SituacaoVeiculo.PENDENTE }
    val meusAlertas = alertas.orEmpty().filter { it.origemEnum == OrigemAlerta.APARELHO }.take(5)

    // Endereço da entrega atual (uma consulta por destino, com cache no serviço).
    var enderecoEntrega by remember { mutableStateOf<Endereco?>(null) }
    LaunchedEffect(aoVivo?.viagem?.destino) {
        val destino = aoVivo?.takeIf { it.emOperacao }?.viagem?.destino ?: return@LaunchedEffect
        enderecoEntrega = runCatching { GeocodingService.endereco(destino) }.getOrNull()
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Espaco.xl),
        verticalArrangement = Arrangement.spacedBy(Espaco.lg)
    ) {
        item {
            TopoAba(
                titulo = "${saudacao()}, ${usuario?.nome?.substringBefore(" ").orEmpty()}",
                subtitulo = "Motorista",
                usuario = usuario,
                onAbrirPerfil = onAbrirPerfil
            )
            ChipSincronizacao(isOnline, status, pendentes, syncViewModel::sincronizarAgora, Modifier.padding(horizontal = Espaco.tela))
        }
        item { CercasDoAparelho() }

        if (meuVeiculo == null) {
            item {
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    EstadoVazio(
                        Icons.Filled.LocalShipping,
                        "Aguardando um veículo",
                        if (propostas.isEmpty()) "O gestor atribui o veículo do seu turno. Se você está com um veículo novo, cadastre-o para aprovação."
                        else "Seu cadastro de ${propostas.joinToString { it.placa }} está com o gestor para aprovação.",
                        acao = if (propostas.isEmpty()) "Cadastrar veículo" else null,
                        onAcao = onCadastrarVeiculo
                    )
                }
            }
            return@LazyColumn
        }

        item {
            CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth(), onClick = { onAbrirAba(Rotas.MEU_VEICULO) }) {
                Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                    FotoVeiculo(meuVeiculo.fotoLocalPath, tipoVeiculo(meuVeiculo.tipo).icone, Modifier.size(72.dp))
                    Spacer(Modifier.width(Espaco.md))
                    Column(Modifier.weight(1f)) {
                        Text("Veículo do turno", style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        PlacaBadge(meuVeiculo.placa, compacta = true)
                        Text(meuVeiculo.modelo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            meuVeiculo.ultimaVistoriaEm?.let { "Última vistoria: ${tempoDesde(it)}" } ?: "Nenhuma vistoria registrada",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (meuVeiculo.ultimaVistoriaEm == null) TrakSyncTheme.colors.warning else TrakSyncTheme.colors.textSecondary
                        )
                    }
                    aoVivo?.let { StatusPill(it.status) }
                }
                Row(Modifier.padding(start = Espaco.lg, end = Espaco.lg, bottom = Espaco.lg)) {
                    BotaoSecundario("Fazer vistoria", Icons.Filled.FactCheck, { onAbrirAba(Rotas.MEU_VEICULO) }, Modifier.fillMaxWidth())
                }
            }
        }

        if (aoVivo != null && aoVivo.emOperacao) {
            item {
                SecaoTitulo("Viagem atual", Modifier.padding(horizontal = Espaco.tela))
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    Column(Modifier.padding(Espaco.lg)) {
                        StepperViagem(aoVivo.viagem.etapa)
                        Spacer(Modifier.height(Espaco.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(Espaco.sm))
                            Column {
                                Text(aoVivo.viagem.clienteNome, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    enderecoEntrega?.resumo ?: aoVivo.viagem.destinoNome,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TrakSyncTheme.colors.textSecondary
                                )
                            }
                        }
                        Text(textoViagem(aoVivo.viagem), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Espaco.sm))
                        Row(
                            Modifier.padding(top = Espaco.md).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Espaco.sm)
                        ) {
                            InfoChip(Icons.Filled.Speed, "${aoVivo.telemetria.velocidadeKmh} km/h")
                            InfoChip(Icons.Filled.PowerSettingsNew, if (aoVivo.telemetria.motorLigado) "Motor ligado" else "Motor desligado")
                            InfoChip(Icons.Filled.SensorDoor, if (aoVivo.telemetria.portasAbertas) "Portas abertas" else "Portas fechadas")
                        }
                        Spacer(Modifier.height(Espaco.lg))
                        BotaoPrincipal(
                            "Navegar até a entrega",
                            onClick = {
                                PedidosNavegacao.pedirMapa(PedidoMapa(rotaAte = aoVivo.viagem.destino, rotaDescricao = aoVivo.viagem.clienteNome))
                                onAbrirAba(Rotas.MAPA)
                            },
                            icone = Icons.Filled.Navigation,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(Espaco.sm))
                        BotaoSecundario(
                            "Abrir no Waze",
                            Icons.AutoMirrored.Filled.OpenInNew,
                            { NavegacaoExterna.abrirWaze(context, aoVivo.viagem.destino) },
                            Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (propostas.isNotEmpty()) {
            item {
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, tint = TrakSyncTheme.colors.warning)
                        Spacer(Modifier.width(Espaco.md))
                        Text(
                            "${propostas.size} veículo(s) cadastrado(s) por você aguardando aprovação.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        item {
            SecaoTitulo("Chegadas e saídas (GPS)", Modifier.padding(horizontal = Espaco.tela))
            CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                if (meusAlertas.isEmpty()) {
                    Text(
                        "Quando você entrar ou sair de uma área monitorada (CD, clientes), o registro aparece aqui e o gestor é avisado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TrakSyncTheme.colors.textSecondary,
                        modifier = Modifier.padding(Espaco.lg)
                    )
                } else {
                    Column {
                        meusAlertas.forEachIndexed { i, alerta ->
                            LinhaAlertaCompacta(alerta)
                            if (i < meusAlertas.lastIndex) HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Situação das cercas reais no GPS do motorista, com o pedido de permissão em etapas:
 * notificações (13+), localização precisa e, por fim, "permitir o tempo todo" (10+).
 */
@Composable
private fun CercasDoAparelho() {
    val context = LocalContext.current
    val app = context.applicationContext as RastreadorApp
    val estado by GeofenceAparelho.estado.collectAsStateWithLifecycle()

    // Voltou das configurações do sistema: tenta registrar de novo.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { GeofenceAparelho.tentarDeNovo(context, app.escopo) }

    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        GeofenceAparelho.tentarDeNovo(context, app.escopo)
    }
    val pedirPrimeiroPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        val precisa = r[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (precisa && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pedirSegundoPlano.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            GeofenceAparelho.tentarDeNovo(context, app.escopo)
        }
    }

    when (val e = estado) {
        EstadoCercasAparelho.SemPermissao -> AvisoPermissao(
            icone = Icons.Filled.Radar,
            titulo = "Avise o gestor quando chegar ao cliente",
            mensagem = "Com a localização \"permitir o tempo todo\", o app registra sozinho suas chegadas e saídas das áreas monitoradas, mesmo fechado.",
            botao = "Permitir localização",
            onPermitir = {
                val permissoes = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                pedirPrimeiroPlano.launch(permissoes.toTypedArray())
            },
            modifier = Modifier.padding(horizontal = Espaco.tela)
        )
        is EstadoCercasAparelho.Ativas -> Row(
            Modifier.padding(horizontal = Espaco.tela),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill("${e.quantidade} área(s) monitorada(s) pelo seu GPS", TrakSyncTheme.colors.success, Icons.Filled.Radar)
        }
        is EstadoCercasAparelho.Erro -> Text(
            e.mensagem,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = Espaco.tela)
        )
        else -> Unit
    }
}

/** "há 2 dias", "hoje", "ontem". */
fun tempoDesde(instante: Long): String {
    val dias = ((System.currentTimeMillis() - instante) / 86_400_000L).toInt()
    return when {
        dias <= 0 -> "hoje"
        dias == 1 -> "ontem"
        else -> "há $dias dias"
    }
}
