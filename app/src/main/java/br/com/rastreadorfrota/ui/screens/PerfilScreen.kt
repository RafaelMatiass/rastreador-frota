package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import br.com.rastreadorfrota.auth.PerfilUiState
import br.com.rastreadorfrota.auth.PerfilViewModel
import br.com.rastreadorfrota.ui.components.AvatarUsuario
import br.com.rastreadorfrota.ui.components.BotoesFoto
import br.com.rastreadorfrota.ui.components.CamposUsuario
import br.com.rastreadorfrota.ui.components.FotoLocal
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val context = LocalContext.current
    val state = viewModel.uiState
    val usuario = viewModel.usuario
    val salvando = state is PerfilUiState.Salvando
    var versaoFoto by remember { mutableIntStateOf(0) }
    var confirmarSaida by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Meu perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
        ) {
            when {
                state is PerfilUiState.Carregando -> CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Center)
                )
                state is PerfilUiState.Erro || usuario == null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        (state as? PerfilUiState.Erro)?.mensagem ?: "Perfil indisponível.",
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = viewModel::carregar) { Text("Tentar novamente") }
                    TextButton(onClick = { viewModel.logout(); onLogout() }) { Text("Sair") }
                }
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    AvatarUsuario(uid = usuario.uid, nome = usuario.nome, tamanho = 104.dp, versao = versaoFoto)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        usuario.nome,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(usuario.perfil.label, style = MaterialTheme.typography.labelLarge, color = TrakSyncTheme.colors.cyan)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = TrakSyncTheme.colors.textSecondary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(usuario.email, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    BotoesFoto(
                        onFotoEscolhida = { uri ->
                            if (FotoLocal.copiar(context, uri, FotoLocal.arquivoPerfil(context, usuario.uid))) {
                                versaoFoto++
                            }
                        }
                    )
                    Text(
                        "A foto fica salva apenas neste aparelho.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrakSyncTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = TrakSyncTheme.colors.surface2)

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
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.ehErro) MaterialTheme.colorScheme.error else TrakSyncTheme.colors.success,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    if (salvando) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = viewModel::descartarAlteracoes,
                                enabled = viewModel.houveAlteracao,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Descartar")
                            }
                            Button(
                                onClick = viewModel::salvar,
                                enabled = viewModel.houveAlteracao,
                                shape = MaterialTheme.shapes.medium,
                                colors = trakSyncPrimaryButtonColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            ) {
                                Text("Salvar", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp), color = TrakSyncTheme.colors.surface2)

                    OutlinedButton(
                        onClick = { confirmarSaida = true },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sair da conta")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmarSaida) {
        AlertDialog(
            onDismissRequest = { confirmarSaida = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará entrar novamente com email e senha.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSaida = false
                    viewModel.logout()
                    onLogout()
                }) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSaida = false }) { Text("Cancelar") }
            }
        )
    }
}
