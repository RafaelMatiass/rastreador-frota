package br.com.rastreadorfrota.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.LoginUiState
import br.com.rastreadorfrota.auth.LoginViewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.StatusPill
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors

@Composable
fun LoginScreen(
    aviso: String?,
    onEntrou: (Usuario) -> Unit,
    onCriarConta: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.uiState
    val foco = LocalFocusManager.current
    var mostrarSenha by rememberSaveable { mutableStateOf(false) }
    val carregando = state is LoginUiState.Loading

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) onEntrou(state.usuario)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.20f), MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espaco.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            Marca()
            Spacer(Modifier.height(Espaco.sm))
            Text(
                "Cada veículo. Uma visão.",
                style = MaterialTheme.typography.bodyLarge,
                color = TrakSyncTheme.colors.textSecondary
            )
            Spacer(Modifier.height(Espaco.md))
            StatusPill("Gestão de frota para distribuidoras", TrakSyncTheme.colors.cyan)

            Spacer(Modifier.height(40.dp))

            Column(Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
                Text("Entrar", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Use o email e a senha da sua conta TrakSync.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TrakSyncTheme.colors.textSecondary
                )
                Spacer(Modifier.height(Espaco.xl))

                OutlinedTextField(
                    value = viewModel.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    singleLine = true,
                    enabled = !carregando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { foco.moveFocus(FocusDirection.Down) }),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Espaco.md))
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Senha") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { mostrarSenha = !mostrarSenha }) {
                            Icon(
                                if (mostrarSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (mostrarSenha) "Ocultar senha" else "Mostrar senha"
                            )
                        }
                    },
                    singleLine = true,
                    enabled = !carregando,
                    visualTransformation = if (mostrarSenha) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { foco.clearFocus(); viewModel.login() }),
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = viewModel::recuperarSenha,
                    enabled = !carregando,
                    modifier = Modifier.align(Alignment.End)
                ) { Text("Esqueci minha senha") }

                // Erro (ou aviso da sessão) num espaço fixo: não empurra o botão de lugar.
                val mensagem = (state as? LoginUiState.Error)?.message ?: aviso.takeIf { state is LoginUiState.Idle }
                AnimatedVisibility(visible = mensagem != null) {
                    Text(
                        mensagem.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Espaco.md)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                BotaoPrincipal(
                    texto = "Entrar",
                    onClick = { foco.clearFocus(); viewModel.login() },
                    carregando = carregando,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Espaco.lg))
                TextButton(
                    onClick = onCriarConta,
                    enabled = !carregando,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Não tem conta? ", color = TrakSyncTheme.colors.textSecondary)
                    Text("Cadastre-se", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(Espaco.xl))
            Text(
                "Distribuidora Morada do Sol · Araraquara",
                style = MaterialTheme.typography.labelSmall,
                color = TrakSyncTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(Espaco.lg))
        }
    }

    viewModel.mensagemRecuperacao?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::dispensarMensagemRecuperacao,
            icon = { Icon(if (msg.startsWith("Enviamos")) Icons.Filled.Email else Icons.Filled.ErrorOutline, contentDescription = null) },
            title = { Text("Redefinir senha") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::dispensarMensagemRecuperacao) { Text("Ok") } }
        )
    }
}
