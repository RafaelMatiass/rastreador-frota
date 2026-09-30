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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.PerfilViewModel
import br.com.rastreadorfrota.ui.components.CabecalhoPainel
import br.com.rastreadorfrota.ui.components.MetricCard
import br.com.rastreadorfrota.ui.components.SyncCard
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import br.com.rastreadorfrota.ui.viewmodel.MotoristaViewModel
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel

/** Painel do controlador: visão geral da frota, mapa e gestão de cadastros. */
@Composable
fun HomeControladorScreen(
    onAbrirPerfil: () -> Unit,
    onAbrirMapa: () -> Unit,
    onGerenciarMotoristas: () -> Unit,
    onGerenciarVeiculos: () -> Unit,
    perfilViewModel: PerfilViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    veiculoViewModel: VeiculoViewModel = viewModel(),
    motoristaViewModel: MotoristaViewModel = viewModel()
) {
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CabecalhoPainel(
                uid = perfilViewModel.uid,
                nome = perfilViewModel.usuario?.nome.orEmpty(),
                perfilLabel = "Painel do ${Perfil.CONTROLADOR.label.lowercase()}",
                onAbrirPerfil = onAbrirPerfil
            )

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
                onClick = onAbrirMapa,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mapa da frota", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onGerenciarMotoristas,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gerenciar motoristas", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onGerenciarVeiculos,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gerenciar veículos", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(modifier = Modifier.height(24.dp))

            SyncCard(
                isOnline = isOnline,
                status = status,
                onForcarSincronizacao = { syncViewModel.sincronizarAgora() }
            )
        }
    }
}
