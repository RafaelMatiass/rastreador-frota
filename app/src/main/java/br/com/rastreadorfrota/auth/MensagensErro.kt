package br.com.rastreadorfrota.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException

/** Traduz exceções do Firebase em mensagens amigáveis para a UI. */
fun mensagemErroFirebase(e: Exception): String = when (e) {
    is FirebaseAuthWeakPasswordException ->
        "Senha fraca. Use pelo menos 6 caracteres."
    is FirebaseAuthInvalidCredentialsException ->
        "Email ou senha incorretos."
    is FirebaseAuthInvalidUserException ->
        "Não existe usuário cadastrado com esse email."
    is FirebaseAuthUserCollisionException ->
        "Já existe uma conta com esse email."
    is FirebaseNetworkException, is IOException ->
        "Sem conexão com a internet. Verifique sua rede e tente novamente."
    is FirebaseFirestoreException -> when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Sem conexão com a internet. Verifique sua rede e tente novamente."
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Sem permissão no Firestore. Verifique as regras da coleção \"usuarios\"."
        else -> "Erro ao acessar os dados. Tente novamente em instantes."
    }
    else ->
        "Não foi possível concluir. Tente novamente em instantes."
}
