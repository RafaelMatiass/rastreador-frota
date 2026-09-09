package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.AuthRepository
import br.com.rastreadorfrota.ui.viewmodel.SyncStatus
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCadastrarVeiculo: () -> Unit,
    onCadastrarMotorista: () -> Unit,
    syncViewModel: SyncViewModel = viewModel()
) {
    val authRepository = AuthRepository()
    val userEmail = authRepository.currentUser?.email ?: "usuário"

    val isOnline by syncViewModel.isOnline.collectAsState()
    val status by syncViewModel.status.collectAsState()

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.padding(top = 24.dp))
            Text(text = "Bem-vindo(a)!", style = MaterialTheme.typography.titleLarge)
            Text(text = userEmail, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.padding(top = 24.dp))

            Button(onClick = onCadastrarVeiculo, modifier = Modifier.fillMaxWidth()) {
                Text("Cadastrar veículo")
            }
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Button(onClick = onCadastrarMotorista, modifier = Modifier.fillMaxWidth()) {
                Text("Cadastrar motorista")
            }

            Spacer(modifier = Modifier.padding(top = 24.dp))

            SyncCard(
                isOnline = isOnline,
                status = status,
                onSincronizar = { syncViewModel.sincronizarAgora() }
            )

            Spacer(modifier = Modifier.padding(top = 24.dp))

            OutlinedButton(onClick = {
                authRepository.logout()
                onLogout()
            }) {
                Text("Sair")
            }
        }
    }
}

@Composable
private fun SyncCard(
    isOnline: Boolean,
    status: SyncStatus,
    onSincronizar: () -> Unit
) {
    val isSyncing = status is SyncStatus.Syncing

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Linha de status de conectividade
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline — alterações ficam pendentes",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))

            Button(
                onClick = onSincronizar,
                enabled = isOnline && !isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                }
                Icon(
                    imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudSync,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.padding(start = 8.dp))
                Text(if (isSyncing) "Sincronizando..." else "Sincronizar agora")
            }

            Spacer(modifier = Modifier.padding(top = 12.dp))

            when (status) {
                is SyncStatus.Success -> ResultadoSincronizacao(icone = Icons.Default.CheckCircle, cor = Color(0xFF2E7D32), linhas = status.log)
                is SyncStatus.Error -> ResultadoSincronizacao(icone = Icons.Default.Error, cor = Color(0xFFC62828), linhas = listOf(status.mensagem))
                SyncStatus.Idle, SyncStatus.Syncing -> Unit
            }
        }
    }
}

@Composable
private fun ResultadoSincronizacao(
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    cor: Color,
    linhas: List<String>
) {
    Row {
        Icon(imageVector = icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.padding(start = 6.dp))
        Text(
            text = if (linhas.size == 1) linhas.first() else "Última sincronização:",
            style = MaterialTheme.typography.labelMedium,
            color = cor
        )
    }

    if (linhas.size > 1) {
        Box(modifier = Modifier.heightIn(max = 140.dp)) {
            LazyColumn {
                items(linhas) { linha ->
                    Text(
                        text = linha,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}