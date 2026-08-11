package br.com.rastreadorfrota.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.rastreadorfrota.auth.AuthRepository

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    val authRepository = AuthRepository()
    val userEmail = authRepository.currentUser?.email ?: "usuário"

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Bem-vindo(a)!", style = MaterialTheme.typography.titleLarge)
            Text(text = userEmail)

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))

            Button(onClick = {
                authRepository.logout()
                onLogout()
            }) {
                Text("Sair")
            }
        }
    }
}
