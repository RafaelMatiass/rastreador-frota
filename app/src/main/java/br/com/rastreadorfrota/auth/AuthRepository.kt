package br.com.rastreadorfrota.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Para usar um provedor de autenticação, é necessário ativá-lo no Console do Firebase
 *
 * O cadastro de usuários é feito pelo console do Firebase
 * (coleção "usuarios", doc id = uid do Firebase Auth), com um campo "perfil"
 * = "MOTORISTA" ou "CONTROLADOR".
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    /**
     * Autentica com email/senha. Lança exceção em caso de falha
     * (credenciais inválidas, sem rede, etc.) para a camada de UI tratar.
     */
    suspend fun login(email: String, password: String): FirebaseUser {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        return result.user ?: throw IllegalStateException("Login sem retorno de usuário")
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
