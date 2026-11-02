package br.com.rastreadorfrota.ui.screens

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.RegrasFrota
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotoesFoto
import br.com.rastreadorfrota.ui.components.FotoLocal
import br.com.rastreadorfrota.ui.components.FotoVeiculo
import br.com.rastreadorfrota.ui.components.icone
import br.com.rastreadorfrota.ui.components.tipoVeiculo
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors
import br.com.rastreadorfrota.ui.viewmodel.CampoVeiculo
import br.com.rastreadorfrota.ui.viewmodel.FormVeiculo
import br.com.rastreadorfrota.ui.viewmodel.ResultadoSalvar
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cadastro e edição de veículo. Motorista: o cadastro vai para aprovação do gestor, e na
 * edição do veículo dele placa e tipo ficam travados (quem muda isso é o gestor).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VeiculoFormScreen(
    veiculoId: Long?,
    usuario: Usuario?,
    onBack: () -> Unit,
    viewModel: VeiculoViewModel = viewModel()
) {
    val context = LocalContext.current
    val escopo = rememberCoroutineScope()
    var existente by remember { mutableStateOf<VeiculoEntity?>(null) }
    var carregando by remember { mutableStateOf(veiculoId != null) }
    var placa by rememberSaveable { mutableStateOf("") }
    var modelo by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableStateOf(TipoVeiculo.CAMINHAO.name) }
    var capacidade by rememberSaveable { mutableStateOf("") }
    var odometro by rememberSaveable { mutableStateOf("") }
    var observacoes by rememberSaveable { mutableStateOf("") }
    var foto by rememberSaveable { mutableStateOf<String?>(null) }
    var erros by remember { mutableStateOf<Map<CampoVeiculo, String>>(emptyMap()) }
    var salvando by remember { mutableStateOf(false) }

    LaunchedEffect(veiculoId) {
        if (veiculoId == null) return@LaunchedEffect
        viewModel.carregar(veiculoId)?.let { v ->
            existente = v
            placa = v.placa
            modelo = v.modelo
            tipo = v.tipo
            capacidade = v.capacidadeCargaKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()
            odometro = v.odometroKm?.toString().orEmpty()
            observacoes = v.observacoes.orEmpty()
            foto = v.fotoLocalPath
        }
        carregando = false
    }

    val ehGestor = usuario?.perfil == Perfil.CONTROLADOR
    val travarIdentidade = existente != null && !ehGestor

    fun guardarFoto(uri: Uri) {
        escopo.launch {
            val destino = FotoLocal.novoArquivoVeiculo(context)
            val ok = withContext(Dispatchers.IO) { FotoLocal.copiar(context, uri, destino) }
            if (ok) foto = destino.absolutePath
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (veiculoId == null) "Novo veículo" else "Editar veículo") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (carregando) {
            CircularProgressIndicator(Modifier.padding(padding).padding(Espaco.xl))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espaco.tela),
            verticalArrangement = Arrangement.spacedBy(Espaco.md)
        ) {
            if (!ehGestor && existente == null) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                    Row(Modifier.padding(Espaco.md), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.width(Espaco.sm))
                        Text(
                            "O veículo vai para o gestor aprovar. Depois de aprovado, ele pode ser atribuído a você.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Foto: identifica o veículo no app (fica só neste aparelho).
            FotoVeiculo(foto, tipoVeiculo(tipo).icone, Modifier.fillMaxWidth().height(180.dp))
            BotoesFoto(onFotoEscolhida = ::guardarFoto)
            Text(
                "A foto fica salva apenas neste aparelho.",
                style = MaterialTheme.typography.labelSmall,
                color = TrakSyncTheme.colors.textSecondary
            )

            OutlinedTextField(
                value = placa,
                onValueChange = {
                    placa = RegrasFrota.normalizarPlaca(it)
                    erros = erros - CampoVeiculo.PLACA
                },
                label = { Text("Placa") },
                placeholder = { Text("ABC1D23") },
                enabled = !travarIdentidade,
                isError = CampoVeiculo.PLACA in erros,
                supportingText = { Text(erros[CampoVeiculo.PLACA] ?: "Padrão antigo ou Mercosul") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = modelo,
                onValueChange = { modelo = it; erros = erros - CampoVeiculo.MODELO },
                label = { Text("Modelo") },
                placeholder = { Text("Ex.: Mercedes Accelo 1016") },
                isError = CampoVeiculo.MODELO in erros,
                supportingText = erros[CampoVeiculo.MODELO]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Tipo", style = MaterialTheme.typography.titleSmall, color = TrakSyncTheme.colors.textSecondary)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Espaco.sm)) {
                TipoVeiculo.entries.forEach { t ->
                    FilterChip(
                        selected = tipo == t.name,
                        onClick = { tipo = t.name },
                        enabled = !travarIdentidade,
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            OutlinedTextField(
                value = capacidade,
                onValueChange = { capacidade = it; erros = erros - CampoVeiculo.CAPACIDADE },
                label = { Text("Capacidade de carga (kg) · opcional") },
                isError = CampoVeiculo.CAPACIDADE in erros,
                supportingText = erros[CampoVeiculo.CAPACIDADE]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = odometro,
                onValueChange = { odometro = it.filter(Char::isDigit); erros = erros - CampoVeiculo.ODOMETRO },
                label = { Text("Quilometragem atual (km) · opcional") },
                isError = CampoVeiculo.ODOMETRO in erros,
                supportingText = erros[CampoVeiculo.ODOMETRO]?.let { { Text(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = observacoes,
                onValueChange = { observacoes = it },
                label = { Text("Observações · opcional") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            BotaoPrincipal(
                texto = when {
                    existente != null -> "Salvar alterações"
                    ehGestor -> "Adicionar à frota"
                    else -> "Enviar para aprovação"
                },
                carregando = salvando,
                onClick = {
                    val atual = usuario ?: return@BotaoPrincipal
                    salvando = true
                    escopo.launch {
                        val resultado = viewModel.salvar(
                            FormVeiculo(placa, modelo, tipo, capacidade, odometro, observacoes, foto),
                            existente,
                            atual
                        )
                        salvando = false
                        when (resultado) {
                            ResultadoSalvar.Ok -> onBack()
                            is ResultadoSalvar.Erros -> erros = resultado.campos
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Espaco.xl))
        }
    }
}
