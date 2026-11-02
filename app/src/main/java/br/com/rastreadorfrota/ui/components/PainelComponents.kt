package br.com.rastreadorfrota.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.data.sync.SyncStatus
import br.com.rastreadorfrota.ui.theme.TrakSyncTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/**
 * Avatar circular: usa a foto de perfil salva no aparelho, se existir,
 * senão as iniciais do nome. [versao] força recarregar depois de trocar a foto.
 */
@Composable
fun AvatarUsuario(
    uid: String?,
    nome: String,
    tamanho: Dp,
    versao: Int = 0
) {
    val context = LocalContext.current
    val arquivo = uid?.let { FotoLocal.arquivoPerfil(context, it) }?.takeIf { it.exists() }
    if (arquivo != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(arquivo)
                // A foto é sobrescrita no mesmo arquivo: a chave muda junto com a data/versão.
                .memoryCacheKey("${arquivo.path}-${arquivo.lastModified()}-$versao")
                .diskCacheKey("${arquivo.path}-${arquivo.lastModified()}-$versao")
                .build(),
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(tamanho)
                .clip(CircleShape)
        )
    } else {
        val iniciais = nome.split(" ").filter { it.isNotBlank() }.let { partes ->
            listOfNotNull(partes.firstOrNull()?.first(), partes.drop(1).lastOrNull()?.first()).joinToString("").uppercase()
        }.ifBlank { "?" }
        Box(
            modifier = Modifier
                .size(tamanho)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, TrakSyncTheme.colors.cyan)), CircleShape)
                .semantics { contentDescription = "Perfil de $nome" },
            contentAlignment = Alignment.Center
        ) {
            Text(
                iniciais,
                style = if (tamanho >= 64.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Situação da sincronização num chip discreto: online/offline, enviando, pendências e
 * há quanto tempo foi a última. Toque força uma sincronização.
 */
@Composable
fun ChipSincronizacao(
    isOnline: Boolean,
    status: SyncStatus,
    pendentes: Int,
    onSincronizar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Relógio para o "há 2 min" andar sozinho.
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            agora = System.currentTimeMillis()
        }
    }
    val (icone, cor, texto) = when {
        status is SyncStatus.Syncing -> Triple(null, MaterialTheme.colorScheme.primary, "Sincronizando...")
        !isOnline -> Triple(
            Icons.Filled.CloudOff,
            TrakSyncTheme.colors.warning,
            if (pendentes > 0) "Offline · $pendentes pendente(s)" else "Offline · dados salvos no aparelho"
        )
        status is SyncStatus.Error -> Triple(Icons.Filled.ErrorOutline, MaterialTheme.colorScheme.error, "Falha ao sincronizar · tocar para tentar")
        pendentes > 0 -> Triple(Icons.Filled.CloudUpload, TrakSyncTheme.colors.warning, "$pendentes alteração(ões) para enviar")
        status is SyncStatus.Success -> Triple(Icons.Filled.CloudDone, TrakSyncTheme.colors.success, "Sincronizado ${haQuanto(status.instante, agora)}")
        else -> Triple(Icons.Filled.CloudDone, TrakSyncTheme.colors.textSecondary, "Sincronização automática")
    }
    Surface(
        onClick = onSincronizar,
        enabled = isOnline && status !is SyncStatus.Syncing,
        shape = CircleShape,
        color = cor.copy(alpha = 0.12f),
        modifier = modifier
            .heightIn(min = 36.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icone == null) CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = cor)
            else Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelMedium, color = cor, maxLines = 1)
        }
    }
}

private fun haQuanto(instante: Long, agora: Long): String {
    val minutos = (agora - instante) / 60_000
    return when {
        minutos < 1 -> "agora"
        minutos < 60 -> "há $minutos min"
        else -> "há ${minutos / 60} h"
    }
}
