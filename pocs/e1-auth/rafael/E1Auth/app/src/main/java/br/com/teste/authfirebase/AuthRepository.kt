package br.com.teste.authfirebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    suspend fun login(email: String, senha: String): Result<FirebaseUser> {
        return try {
            val resultado = firebaseAuth
                .signInWithEmailAndPassword(email, senha)
                .await()

            val usuario = resultado.user
            if (usuario != null) {
                Result.success(usuario)
            } else {
                Result.failure(Exception("Usuário não encontrado após login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun usuarioAtual(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}