package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.LoginUiState
import br.com.rastreadorfrota.auth.LoginViewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import br.com.rastreadorfrota.ui.theme.trakSyncTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (Perfil) -> Unit,
    onCriarConta: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) {
            onLoginSuccess(state.perfil)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Wordmark: TRAK (branco) + SYNC (ciano)
            Row {
                Text(
                    text = "TRAK",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "SYNC",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TrakSyncTheme.colors.cyan
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Every vehicle. One view.",
                style = MaterialTheme.typography.bodyMedium,
                color = TrakSyncTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            when (state) {
                is LoginUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                is LoginUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::login,
                        shape = MaterialTheme.shapes.medium,
                        colors = trakSyncPrimaryButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Entrar", style = MaterialTheme.typography.labelLarge)
                    }
                }
                else -> {
                    Button(
                        onClick = viewModel::login,
                        shape = MaterialTheme.shapes.medium,
                        colors = trakSyncPrimaryButtonColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Entrar", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onCriarConta,
                enabled = state !is LoginUiState.Loading
            ) {
                Text("Não tem conta? Cadastre-se", color = TrakSyncTheme.colors.cyan)
            }
        }
    }
}