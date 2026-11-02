package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.BuildConfig
import br.com.rastreadorfrota.auth.PerfilUiState
import br.com.rastreadorfrota.auth.PerfilViewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.sync.SyncEngine
import br.com.rastreadorfrota.ui.components.AvatarUsuario
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.BotaoSecundario
import br.com.rastreadorfrota.ui.components.BotoesFoto
import br.com.rastreadorfrota.ui.components.CamposUsuario
import br.com.rastreadorfrota.ui.components.CardTrak
import br.com.rastreadorfrota.ui.components.DialogoConfirmacao
import br.com.rastreadorfrota.ui.components.FotoLocal
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.PreferenciaTema
import br.com.rastreadorfrota.ui.theme.TemaApp
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

/** Meu perfil: foto (só no aparelho), dados cadastrais editáveis e sair da conta. */
@Composable
fun PerfilScreen(
    usuario: Usuario?,
    mostrarVoltar: Boolean,
    onBack: () -> Unit,
    onPerfilAtualizado: (Usuario) -> Unit,
    onSair: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    val salvando = state is PerfilUiState.Salvando
    val pendentes by SyncEngine.pendentes.collectAsState()
    val tema by PreferenciaTema.tema.collectAsState()
    var versaoFoto by remember { mutableIntStateOf(0) }
    var confirmarSaida by remember { mutableStateOf(false) }

    // Salvou: a sessão do app inteiro passa a ver os dados novos.
    LaunchedEffect(viewModel.usuario) {
        val novo = viewModel.usuario
        if (novo != null && usuario != null && novo != usuario) onPerfilAtualizado(novo)
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.padding(start = 4.dp, end = Espaco.tela, top = Espaco.sm), verticalAlignment = Alignment.CenterVertically) {
            if (mostrarVoltar) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
            else Spacer(Modifier.width(Espaco.lg))
            Text("Meu perfil", style = MaterialTheme.typography.headlineSmall)
        }

        val atual = viewModel.usuario
        when {
            state is PerfilUiState.Carregando -> Box(Modifier.fillMaxWidth().padding(Espaco.xxl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state is PerfilUiState.Erro || atual == null -> Column(Modifier.padding(Espaco.xl), horizontalAlignment = Alignment.CenterHorizontally) {
                Text((state as? PerfilUiState.Erro)?.mensagem ?: "Perfil indisponível.", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::carregar) { Text("Tentar novamente") }
                TextButton(onClick = { confirmarSaida = true }) { Text("Sair") }
            }
            else -> Column(Modifier.padding(horizontal = Espaco.tela), verticalArrangement = Arrangement.spacedBy(Espaco.lg)) {
                CardTrak(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(Espaco.lg), horizontalAlignment = Alignment.CenterHorizontally) {
                        AvatarUsuario(uid = atual.uid, nome = atual.nome, tamanho = 96.dp, versao = versaoFoto)
                        Spacer(Modifier.height(Espaco.md))
                        Text(atual.nome, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(Espaco.xs))
                        StatusPill(atual.perfil.label, TrakSyncTheme.colors.cyan)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Espaco.sm)) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = TrakSyncTheme.colors.textSecondary, modifier = Modifier.padding(end = 6.dp))
                            Text(atual.email, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)
                        }
                        Spacer(Modifier.height(Espaco.md))
                        BotoesFoto(onFotoEscolhida = { uri ->
                            if (FotoLocal.copiar(context, uri, FotoLocal.arquivoPerfil(context, atual.uid))) versaoFoto++
                        })
                        Text("A foto fica salva apenas neste aparelho.", style = MaterialTheme.typography.labelSmall, color = TrakSyncTheme.colors.textSecondary)
                    }
                }

                CamposUsuario(
                    formulario = viewModel.formulario,
                    erros = viewModel.erros,
                    onChange = viewModel::onFormularioChange,
                    modoCadastro = false,
                    habilitado = !salvando
                )
                (state as? PerfilUiState.Pronto)?.mensagem?.let { mensagem ->
                    Text(
                        mensagem,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.ehErro) MaterialTheme.colorScheme.error else TrakSyncTheme.colors.success
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Espaco.md)) {
                    BotaoSecundario("Descartar", null, viewModel::descartarAlteracoes, Modifier.weight(1f), enabled = viewModel.houveAlteracao && !salvando)
                    BotaoPrincipal("Salvar", viewModel::salvar, Modifier.weight(1f), enabled = viewModel.houveAlteracao, carregando = salvando)
                }

                CardTrak(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Espaco.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(Espaco.md))
                            Text("Aparência", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(Espaco.md))
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            TemaApp.entries.forEachIndexed { i, opcao ->
                                SegmentedButton(
                                    selected = tema == opcao,
                                    onClick = { PreferenciaTema.definir(context, opcao) },
                                    shape = SegmentedButtonDefaults.itemShape(i, TemaApp.entries.size),
                                    icon = {
                                        Icon(
                                            when (opcao) {
                                                TemaApp.SISTEMA -> Icons.Filled.BrightnessAuto
                                                TemaApp.CLARO -> Icons.Filled.LightMode
                                                TemaApp.ESCURO -> Icons.Filled.DarkMode
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                ) { Text(opcao.rotulo) }
                            }
                        }
                        Text(
                            "\"Sistema\" acompanha o modo claro/escuro do celular.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TrakSyncTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = Espaco.sm)
                        )
                    }
                }

                CardTrak(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(Espaco.lg), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(Espaco.md))
                        Column {
                            Text("TrakSync ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Gestão de frota para distribuidoras · O tema segue o modo claro/escuro do aparelho.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TrakSyncTheme.colors.textSecondary
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmarSaida = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(Espaco.sm))
                    Text("Sair da conta")
                }
                Spacer(Modifier.height(Espaco.xl))
            }
        }
    }

    if (confirmarSaida) {
        DialogoConfirmacao(
            titulo = "Sair da conta?",
            mensagem = if (pendentes > 0)
                "Há $pendentes alteração(ões) ainda não enviada(s) para a nuvem. Ao sair, elas serão perdidas. Conecte-se à internet antes, se possível."
            else "Os dados deste aparelho serão limpos. Você precisará entrar de novo com email e senha.",
            confirmar = "Sair",
            destrutivo = true,
            icone = Icons.AutoMirrored.Filled.Logout,
            onConfirmar = {
                confirmarSaida = false
                onSair()
            },
            onCancelar = { confirmarSaida = false }
        )
    }
}
