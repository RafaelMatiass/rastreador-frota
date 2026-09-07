package com.example.pocesync

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncRepository(
    private val produtoDao: ProdutoDao
) {

    private val firestore = FirebaseFirestore.getInstance()

    suspend fun sincronizar(): SyncResult {
        val pendentes = produtoDao.buscarNaoSincronizados()

        if (pendentes.isEmpty()) {
            return SyncResult(0, "Nenhum produto pendente.")
        }

        var sincronizados = 0

        for (produto in pendentes) {
            val dados = mapOf(
                "id" to produto.id,
                "nome" to produto.nome,
                "preco" to produto.preco
            )

            firestore
                .collection("produtos")
                .document(produto.id)
                .set(dados)
                .await()

            produtoDao.marcarComoSincronizado(produto.id)
            sincronizados++
        }

        return SyncResult(
            sincronizados,
            "$sincronizados produto(s) sincronizado(s)."
        )
    }
}

data class SyncResult(
    val quantidade: Int,
    val mensagem: String
)
