package br.com.rastreadorfrota.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.PerfilViewModel
import br.com.rastreadorfrota.data.local.entity.TipoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import java.io.File
import br.com.rastreadorfrota.ui.components.CabecalhoPainel
import br.com.rastreadorfrota.ui.components.SyncCard
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.theme.trakSyncPrimaryButtonColors
import br.com.rastreadorfrota.ui.viewmodel.SyncStatus
import br.com.rastreadorfrota.ui.viewmodel.SyncViewModel
import br.com.rastreadorfrota.ui.viewmodel.VeiculoViewModel

/** Painel do motorista: foco em cadastrar/fotografar o veículo. */
@Composable
fun HomeMotoristaScreen(
    onAbrirPerfil: () -> Unit,
    onCadastrarVeiculo: () -> Unit,
    onAbrirMapa: (veiculoRemoteId: String?) -> Unit,
    perfilViewModel: PerfilViewModel = viewModel(),
    syncViewModel: SyncViewModel = viewModel(),
    veiculoViewModel: VeiculoViewModel = viewModel()
) {
    val isOnline by syncViewModel.isOnline.collectAsState()
    val status by syncViewModel.status.collectAsState()
    val veiculos by veiculoViewModel.veiculos.collectAsState()
    val meuVeiculo = perfilViewModel.usuario?.veiculoRemoteId
        ?.let { remoteId -> veiculos.find { it.remoteId == remoteId } }

    // O veículo é associado pelo controlador em outro aparelho: a cada
    // sincronização concluída, relê o próprio perfil pra refletir a mudança.
    LaunchedEffect(status) {
        if (status is SyncStatus.Success) perfilViewModel.carregar()
    }

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
                perfilLabel = Perfil.MOTORISTA.label,
                onAbrirPerfil = onAbrirPerfil
            )

            Spacer(modifier = Modifier.height(24.dp))

            MeuVeiculoCard(meuVeiculo)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCadastrarVeiculo,
                shape = MaterialTheme.shapes.medium,
                colors = trakSyncPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cadastrar veículo", style = MaterialTheme.typography.labelLarge)
            }
            Text(
                "Tire uma foto do veículo para identificá-lo no app. A foto fica salva só neste aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = TrakSyncTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = { onAbrirMapa(perfilViewModel.usuario?.veiculoRemoteId) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver meu veículo no mapa", style = MaterialTheme.typography.labelLarge)
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

@Composable
private fun MeuVeiculoCard(veiculo: VeiculoEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // A foto só existe no aparelho em que o veículo foi fotografado.
            val foto = remember(veiculo?.fotoLocalPath) {
                veiculo?.fotoLocalPath
                    ?.takeIf { File(it).exists() }
                    ?.let { BitmapFactory.decodeFile(it) }
            }
            if (foto != null) {
                Image(
                    bitmap = foto.asImageBitmap(),
                    contentDescription = "Foto do veículo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Icon(
                    Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text("Meu veículo", style = MaterialTheme.typography.labelMedium, color = TrakSyncTheme.colors.textSecondary)
                if (veiculo != null) {
                    Text(veiculo.placa, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "${veiculo.modelo} · ${TipoVeiculo.entries.find { it.name == veiculo.tipo }?.label ?: veiculo.tipo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrakSyncTheme.colors.textSecondary
                    )
                } else {
                    Text(
                        "Nenhum veículo associado",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "O controlador associa um veículo a você.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TrakSyncTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
