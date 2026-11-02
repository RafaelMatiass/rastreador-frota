package br.com.rastreadorfrota.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.BotoesFoto
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.EstadoVazio
import br.com.rastreadorfrota.ui.components.FotoLocal
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.LinhaDado
import br.com.rastreadorfrota.ui.components.PlacaBadge
import br.com.rastreadorfrota.ui.components.SecaoTitulo
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.components.TopoAba
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Aba "Veículo" do motorista: o veículo do turno (com a vistoria) e os veículos que ele
 * cadastrou e aguardam aprovação. Ele não vê nem mexe no resto da frota.
 */
@Composable
fun MeuVeiculoScreen(
    usuario: Usuario?,
    onCadastrarVeiculo: () -> Unit,
    onEditarVeiculo: (Long) -> Unit,
    onAbrirMapa: () -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val veiculos by viewModel.veiculos.collectAsStateWithLifecycle()
    var vistoria by rememberSaveable { mutableStateOf(false) }
    val meu = usuario?.veiculoRemoteId?.let { id -> veiculos.find { it.remoteId == id } }
    val propostas = veiculos.filter { it.cadastradoPorUid == usuario?.uid && it.id != meu?.id }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Espaco.xl),
        verticalArrangement = Arrangement.spacedBy(Espaco.lg)
    ) {
        item { TopoAba("Meu veículo", null, {}, subtitulo = "Vistoria e cadastro") }

        if (meu == null) {
            item {
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    EstadoVazio(
                        Icons.Filled.LocalShipping,
                        "Nenhum veículo atribuído",
                        "Cadastre o veículo com que você vai trabalhar. O gestor aprova e atribui a você.",
                        acao = "Cadastrar veículo",
                        onAcao = onCadastrarVeiculo
                    )
                }
            }
        } else {
            item {
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    Column {
                        FotoVeiculo(meu.fotoLocalPath, tipoVeiculo(meu.tipo).icone, Modifier.fillMaxWidth().height(190.dp))
                        Column(Modifier.padding(Espaco.lg)) {
                            Row {
                                PlacaBadge(meu.placa)
                                Spacer(Modifier.weight(1f))
                                StatusPill(meu.situacaoEnum)
                            }
                            Text(meu.modelo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = Espaco.sm))
                            Text(tipoVeiculo(meu.tipo).label, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)
                            Spacer(Modifier.height(Espaco.md))
                            BotaoPrincipal("Fazer vistoria", { vistoria = true }, Modifier.fillMaxWidth(), icone = Icons.Filled.FactCheck)
                            Spacer(Modifier.height(Espaco.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                                BotaoSecundario("Editar dados", Icons.Filled.Edit, { onEditarVeiculo(meu.id) }, Modifier.weight(1f))
                                BotaoSecundario("Ver no mapa", Icons.Filled.Map, onAbrirMapa, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item {
                SecaoTitulo("Dados", Modifier.padding(horizontal = Espaco.tela))
                CardTrak(Modifier.padding(horizontal = Espaco.tela).fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = Espaco.lg, vertical = Espaco.sm)) {
                        LinhaDado("Última vistoria", meu.ultimaVistoriaEm?.let(::tempoDesde) ?: "Nenhuma")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Quilometragem", meu.odometroKm?.let { "$it km" } ?: "—")
                        HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                        LinhaDado("Capacidade", meu.capacidadeCargaKg?.let { "${it.toInt()} kg" } ?: "—")
                        meu.observacoes?.let {
                            HorizontalDivider(color = TrakSyncTheme.colors.surface2)
                            LinhaDado("Observações", it)
                        }
                    }
                }
            }
        }

        if (propostas.isNotEmpty()) {
            item { SecaoTitulo("Cadastrados por você", Modifier.padding(horizontal = Espaco.tela)) }
            items(propostas, key = { it.id }) { v ->
                CardVeiculoFrota(
                    veiculo = v,
                    motorista = null,
                    onClick = { if (v.situacaoEnum == SituacaoVeiculo.PENDENTE) onEditarVeiculo(v.id) },
                    onAprovar = null,
                    modifier = Modifier.padding(horizontal = Espaco.tela)
                )
            }
        }
        if (meu != null || propostas.isNotEmpty()) {
            item {
                BotaoSecundario(
                    "Cadastrar outro veículo",
                    Icons.Filled.Add,
                    onCadastrarVeiculo,
                    Modifier.fillMaxWidth().padding(horizontal = Espaco.tela)
                )
            }
        }
    }

    if (vistoria && meu != null) {
        FolhaVistoria(meu, viewModel) { vistoria = false }
    }
}

/** Vistoria rápida: foto atual do veículo, quilometragem e observação. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolhaVistoria(veiculo: VeiculoEntity, viewModel: VeiculoViewModel, onFechar: () -> Unit) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var foto by remember { mutableStateOf<String?>(null) }
    var km by remember { mutableStateOf(veiculo.odometroKm?.toString().orEmpty()) }
    var obs by remember { mutableStateOf("") }

    fun guardar(uri: Uri) {
        escopo.launch {
            val destino = FotoLocal.novoArquivoVeiculo(context)
            if (withContext(Dispatchers.IO) { FotoLocal.copiar(context, uri, destino) }) foto = destino.absolutePath
        }
    }

    ModalBottomSheet(onDismissRequest = onFechar, sheetState = estado, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = Espaco.tela).padding(bottom = Espaco.xl), verticalArrangement = Arrangement.spacedBy(Espaco.md)) {
            Text("Vistoria de ${veiculo.placa}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tire uma foto do veículo como ele está agora e informe a quilometragem do painel.",
                style = MaterialTheme.typography.bodyMedium,
                color = TrakSyncTheme.colors.textSecondary
            )
            FotoVeiculo(foto ?: veiculo.fotoLocalPath, tipoVeiculo(veiculo.tipo).icone, Modifier.fillMaxWidth().height(160.dp))
            BotoesFoto(onFotoEscolhida = ::guardar)
            OutlinedTextField(
                value = km,
                onValueChange = { km = it.filter(Char::isDigit) },
                label = { Text("Quilometragem (km)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = obs,
                onValueChange = { obs = it },
                label = { Text("Observações (avarias, pneus, limpeza...)") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            BotaoPrincipal(
                "Registrar vistoria",
                onClick = {
                    viewModel.registrarVistoria(veiculo, foto, km.toIntOrNull(), obs)
                    onFechar()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
