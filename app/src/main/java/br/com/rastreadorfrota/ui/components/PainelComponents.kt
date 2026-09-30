package br.com.rastreadorfrota.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import br.com.rastreadorfrota.ui.viewmodel.SyncStatus

/**
 * Avatar circular: usa a foto de perfil salva no aparelho, se existir,
 * senão a inicial do nome. [versao] força recarregar depois de trocar a foto.
 */
@Composable
fun AvatarUsuario(
    uid: String?,
    nome: String,
    tamanho: Dp,
    versao: Int = 0
) {
    val context = LocalContext.current
    val arquivo = uid?.let { FotoLocal.arquivoPerfil(context, it) }
    val modificado = arquivo?.takeIf { it.exists() }?.lastModified() ?: 0L
    val bitmap = remember(arquivo?.path, modificado, versao) {
        arquivo?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(tamanho)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(tamanho)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                nome.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun MetricCard(
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
fun SyncCard(
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
/** Topo das telas iniciais: avatar + saudação. Toque abre o "Meu perfil". */
@Composable
fun CabecalhoPainel(
    uid: String?,
    nome: String,
    perfilLabel: String,
    onAbrirPerfil: () -> Unit
) {
    Card(
        onClick = onAbrirPerfil,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = TrakSyncTheme.colors.surface2)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarUsuario(uid = uid, nome = nome, tamanho = 52.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Olá, ${nome.substringBefore(" ").ifBlank { "usuário" }}!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(perfilLabel, style = MaterialTheme.typography.bodySmall, color = TrakSyncTheme.colors.cyan)
            }
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Meu perfil",
                tint = TrakSyncTheme.colors.textSecondary
            )
        }
    }
}
