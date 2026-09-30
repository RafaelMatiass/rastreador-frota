package br.com.rastreadorfrota.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Para usar um provedor de autenticação, é necessário ativá-lo no Console do Firebase
 *
 * Os dados do usuário ficam na coleção "usuarios" (doc id = uid do Firebase Auth),
 * com um campo "perfil" = "MOTORISTA" ou "CONTROLADOR" que decide a tela inicial.
 */
class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecaoUsuarios = firestore.collection("usuarios")

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

    /**
     * Cria a conta no Firebase Auth e grava os dados em "usuarios/{uid}".
     * Se a gravação no Firestore falhar, a conta recém-criada é apagada para
     * não sobrar um login sem perfil (que não conseguiria entrar no app).
     */
    suspend fun cadastrar(dados: Usuario, senha: String): Usuario {
        val user = firebaseAuth.createUserWithEmailAndPassword(dados.email, senha).await().user
            ?: throw IllegalStateException("Cadastro sem retorno de usuário")
        val usuario = dados.copy(uid = user.uid)
        try {
            colecaoUsuarios.document(user.uid).set(usuario.toMap() + (Usuario.CAMPO_ATIVO to true)).await()
        } catch (e: Exception) {
            runCatching { user.delete().await() }
            throw e
        }
        return usuario
    }

    /** Busca os dados do usuário logado. Retorna null se não houver perfil válido. */
    suspend fun buscarUsuarioAtual(): Usuario? {
        val uid = currentUser?.uid ?: return null
        val doc = colecaoUsuarios.document(uid).get().await()
        return Usuario.fromDocument(doc)
    }

    /** Email e perfil não são alterados aqui: só os dados cadastrais. */
    suspend fun atualizarUsuario(usuario: Usuario) {
        val campos = usuario.toMap() - listOf("email", "perfil")
        colecaoUsuarios.document(usuario.uid).set(campos, SetOptions.merge()).await()
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}
