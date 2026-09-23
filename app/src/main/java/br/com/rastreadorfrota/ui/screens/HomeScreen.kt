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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.AuthRepository
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import br.com.rastreadorfrota.ui.viewmodel.SyncStatus
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel
import br.com.rastreadorfrota.ui.viewmodel.MotoristaViewModel

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onCadastrarVeiculo: () -> Unit,
    onCadastrarMotorista: () -> Unit,
    onAbrirMapa: () -> Unit,
    syncViewModel: SyncViewModel = viewModel(),
    veiculoViewModel: VeiculoViewModel = viewModel(),
    motoristaViewModel: MotoristaViewModel = viewModel()
) {
    val authRepository = AuthRepository()
    val userEmail = authRepository.currentUser?.email ?: "usuário"
    val inicial = userEmail.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    val isOnline by syncViewModel.isOnline.collectAsState()
    val status by syncViewModel.status.collectAsState()
    val veiculos by veiculoViewModel.veiculos.collectAsState()
    val motoristas by motoristaViewModel.motoristas.collectAsState()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    inicial,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Bem-vindo(a)!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = TrakSyncTheme.colors.textSecondary)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Visão geral da operação",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    icon = Icons.Default.DirectionsCar,
                    value = veiculos.size.toString(),
                    label = "Veículos",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    icon = Icons.Default.Person,
                    value = motoristas.size.toString(),
                    label = "Motoristas",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCadastrarVeiculo,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Cadastrar veículo", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCadastrarMotorista,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Cadastrar motorista", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAbrirMapa,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir mapa da frota", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SyncCard(
                isOnline = isOnline,
                status = status,
                onForcarSincronizacao = { syncViewModel.sincronizarAgora() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    authRepository.logout()
                    onLogout()
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TrakSyncTheme.colors.textSecondary)
            ) {
                Text("Sair")
            }
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.textSecondary)
        }
    }
}

@Composable
private fun SyncCard(
    isOnline: Boolean,
    status: SyncStatus,
    onForcarSincronizacao: () -> Unit
) {
    val isSyncing = status is SyncStatus.Syncing

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Conectividade
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOnline) TrakSyncTheme.colors.success else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline — alterações ficam pendentes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status da sincronização automática (sem precisar clicar em nada)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isSyncing -> "Sincronizando..."
                        isOnline -> "Sincronização automática ativa"
                        else -> "Aguardando conexão para sincronizar"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (status) {
                is SyncStatus.Success -> ResultadoSincronizacao(
                    icone = Icons.Default.CheckCircle,
                    cor = TrakSyncTheme.colors.success,
                    linhas = status.log
                )
                is SyncStatus.Error -> ResultadoSincronizacao(
                    icone = Icons.Default.Error,
                    cor = MaterialTheme.colorScheme.error,
                    linhas = listOf(status.mensagem)
                )
                SyncStatus.Idle, SyncStatus.Syncing -> Unit
            }

            // Opção manual, discreta, só como recurso extra (ex: testes em aula)
            TextButton(
                onClick = onForcarSincronizacao,
                enabled = isOnline && !isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Forçar sincronização agora", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ResultadoSincronizacao(
    icone: ImageVector,
    cor: Color,
    linhas: List<String>
) {
    Row {
        Icon(imageVector = icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
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
                        color = TrakSyncTheme.colors.textSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}