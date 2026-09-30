package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.CadastroUsuarioUiState
import br.com.rastreadorfrota.auth.CadastroUsuarioViewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.ui.components.CamposUsuario
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroUsuarioScreen(
    onBack: () -> Unit,
    onCadastroConcluido: (Perfil) -> Unit,
    viewModel: CadastroUsuarioViewModel = viewModel()
) {
    val state = viewModel.uiState
    val carregando = state is CadastroUsuarioUiState.Loading

    LaunchedEffect(state) {
        if (state is CadastroUsuarioUiState.Success) onCadastroConcluido(state.perfil)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Criar conta") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CamposUsuario(
                formulario = viewModel.formulario,
                erros = viewModel.erros,
                onChange = viewModel::onFormularioChange,
                modoCadastro = true,
                habilitado = !carregando
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (state is CadastroUsuarioUiState.Error) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (carregando) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                Button(
                    onClick = viewModel::cadastrar,
                    shape = MaterialTheme.shapes.medium,
                    colors = trakSyncPrimaryButtonColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Cadastrar", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
