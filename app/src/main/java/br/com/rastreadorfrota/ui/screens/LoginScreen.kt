package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.LoginUiState
import br.com.rastreadorfrota.auth.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state = viewModel.uiState

    LaunchedEffect(state) {
        if (state is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Rastreador de Frota", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))

            OutlinedTextField(
                value = viewModel.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Senha") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 20.dp))

            when (state) {
                is LoginUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is LoginUiState.Error -> {
                    Text(
                        text = state.message,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = viewModel::login, modifier = Modifier.fillMaxWidth()) {
                        Text("Entrar")
                    }
                }
                else -> {
                    Button(onClick = viewModel::login, modifier = Modifier.fillMaxWidth()) {
                        Text("Entrar")
                    }
                }
            }
        }
    }
}
