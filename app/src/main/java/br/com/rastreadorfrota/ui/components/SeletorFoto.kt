package br.com.rastreadorfrota.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * Fotos ficam SOMENTE no armazenamento interno privado do app (filesDir).
 * Nada disso vai para o Firestore: na nuvem não existe referência à imagem.
 */
object FotoLocal {
    fun arquivoPerfil(context: Context, uid: String): File =
        File(File(context.filesDir, "profile_photos").apply { mkdirs() }, "$uid.jpg")

    fun novoArquivoVeiculo(context: Context): File =
        File(File(context.filesDir, "vehicle_photos").apply { mkdirs() }, "vehicle_${System.currentTimeMillis()}.jpg")

    /** Copia o conteúdo do [uri] (câmera ou galeria) para [destino]. */
    fun copiar(context: Context, uri: Uri, destino: File): Boolean =
        context.contentResolver.openInputStream(uri)?.use { input ->
            destino.outputStream().use { output -> input.copyTo(output) }
            true
        } ?: false
}

/** Botões "Câmera" e "Galeria". Devolve o Uri temporário da imagem escolhida. */
@Composable
fun BotoesFoto(
    onFotoEscolhida: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onFotoEscolhida) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        if (captured) cameraUri?.let(onFotoEscolhida)
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                val imageFile = File(context.cacheDir, "images").apply { mkdirs() }
                    .resolve("capture.jpg")
                cameraUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                cameraUri?.let(cameraLauncher::launch)
            },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text("Câmera")
        }
        OutlinedButton(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.size(6.dp))
            Text("Galeria")
        }
    }
}
