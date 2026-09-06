package com.example.pocsqlite_otavio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.pocsqlite_otavio.data.AppDatabase
import com.example.pocsqlite_otavio.data.NotaEntity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(applicationContext)

        setContent {
            MaterialTheme {
                val notas by db.notaDao().observarTodas().collectAsState(initial = emptyList())
                var texto by remember { mutableStateOf("") }

                Scaffold { padding ->
                    Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                        Text("PoC SQLite (Room)", style = MaterialTheme.typography.titleLarge)

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            label = { Text("Digite uma nota") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (texto.isNotBlank()) {
                                    lifecycleScope.launch {
                                        db.notaDao().inserir(NotaEntity(texto = texto))
                                    }
                                    texto = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Salvar no banco")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Notas salvas (${notas.size}):", style = MaterialTheme.typography.titleMedium)

                        LazyColumn {
                            items(notas, key = { it.id }) { nota ->
                                Text("• ${nota.texto}", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}