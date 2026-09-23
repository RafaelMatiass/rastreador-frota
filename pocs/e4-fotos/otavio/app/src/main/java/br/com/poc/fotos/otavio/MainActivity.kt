package br.com.poc.fotos.otavio

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FotosPoc() } }
    }
}

@Composable
private fun FotosPoc() {
    val context = LocalContext.current
    var savedPath by remember { mutableStateOf<String?>(null) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    fun copyToInternalStorage(uri: Uri) {
        val directory = File(context.filesDir, "poc_photos").apply { mkdirs() }
        val destination = File(directory, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
            savedPath = destination.absolutePath
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::copyToInternalStorage)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingUri?.let(::copyToInternalStorage)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("PoC: foto no armazenamento local", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("A imagem e copiada para filesDir/poc_photos, sem nuvem.")
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val image = File(context.cacheDir, "images").apply { mkdirs() }
                    .resolve("capture.jpg")
                pendingUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", image
                )
                pendingUri?.let(camera::launch)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Tirar foto") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { gallery.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Escolher da galeria") }
        Spacer(Modifier.height(16.dp))
        savedPath?.let { path ->
            BitmapFactory.decodeFile(path)?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto salva localmente",
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }
            Text("Salva localmente em: $path")
        }
    }
}
