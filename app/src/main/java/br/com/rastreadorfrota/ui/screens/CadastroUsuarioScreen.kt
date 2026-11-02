package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.CadastroUsuarioUiState
import br.com.rastreadorfrota.auth.CadastroUsuarioViewModel
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.ui.components.BotaoPrincipal
import br.com.rastreadorfrota.ui.components.CamposUsuario
import br.com.rastreadorfrota.ui.theme.Espaco
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroUsuarioScreen(
    onBack: () -> Unit,
    onCadastroConcluido: (Usuario) -> Unit,
    viewModel: CadastroUsuarioViewModel = viewModel()
) {
    val state = viewModel.uiState
    val carregando = state is CadastroUsuarioUiState.Loading

    LaunchedEffect(state) {
        if (state is CadastroUsuarioUiState.Success) onCadastroConcluido(state.usuario)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Espaco.tela)
        ) {
            Text(
                "Leva um minuto. Motoristas recebem o veículo do gestor depois de entrar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TrakSyncTheme.colors.textSecondary
            )
            Spacer(Modifier.height(Espaco.lg))
            CamposUsuario(
                formulario = viewModel.formulario,
                erros = viewModel.erros,
                onChange = viewModel::onFormularioChange,
                modoCadastro = true,
                habilitado = !carregando
            )

            Spacer(Modifier.height(Espaco.xl))

            if (state is CadastroUsuarioUiState.Error) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(bottom = Espaco.md)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            BotaoPrincipal(
                texto = "Criar conta",
                onClick = viewModel::cadastrar,
                carregando = carregando,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Espaco.xl))
        }
    }
}
