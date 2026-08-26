package br.com.pocteste.authfirebase

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var email by remember { mutableStateOf("") }
            var senha by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {

                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") }
                )

                TextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") }
                )

                Button(
                    onClick = {
                        auth.signInWithEmailAndPassword(email, senha)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Login realizado!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Erro: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                ) {
                    Text("Testar Login")
                }
            }
        }
    }
}