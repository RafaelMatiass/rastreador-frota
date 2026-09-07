package com.example.pocesync

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var produtoDao: ProdutoDao
    private lateinit var syncRepository: SyncRepository

    private lateinit var edtNome: EditText
    private lateinit var edtPreco: EditText
    private lateinit var txtStatus: TextView
    private lateinit var txtProdutos: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val database = AppDatabase.getInstance(this)
        produtoDao = database.produtoDao()
        syncRepository = SyncRepository(produtoDao)

        edtNome = findViewById(R.id.edtNome)
        edtPreco = findViewById(R.id.edtPreco)
        txtStatus = findViewById(R.id.txtStatus)
        txtProdutos = findViewById(R.id.txtProdutos)

        findViewById<Button>(R.id.btnSalvar).setOnClickListener {
            salvarLocalmente()
        }

        findViewById<Button>(R.id.btnSincronizar).setOnClickListener {
            sincronizar()
        }

        findViewById<Button>(R.id.btnListar).setOnClickListener {
            atualizarLista()
        }

        atualizarLista()
    }

    private fun salvarLocalmente() {
        val nome = edtNome.text.toString().trim()
        val preco = edtPreco.text.toString()
            .replace(",", ".")
            .toDoubleOrNull()

        if (nome.isEmpty() || preco == null) {
            Toast.makeText(
                this,
                "Informe nome e preço.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            val produto = ProdutoEntity(
                id = UUID.randomUUID().toString(),
                nome = nome,
                preco = preco,
                sincronizado = false
            )

            produtoDao.inserir(produto)

            edtNome.text.clear()
            edtPreco.text.clear()

            txtStatus.text = "Status: salvo somente no SQLite."

            atualizarLista()
        }
    }

    private fun sincronizar() {
        lifecycleScope.launch {
            txtStatus.text = "Status: sincronizando..."

            try {
                val resultado = syncRepository.sincronizar()

                txtStatus.text = "Status: ${resultado.mensagem}"

                atualizarLista()

            } catch (e: Exception) {
                txtStatus.text =
                    "Status: falha na sincronização.\n" +
                            (e.message ?: "Sem conexão.")

                atualizarLista()
            }
        }
    }

    private fun atualizarLista() {
        lifecycleScope.launch {
            val produtos = produtoDao.buscarTodos()

            if (produtos.isEmpty()) {
                txtProdutos.text = "Nenhum produto."
                return@launch
            }

            txtProdutos.text = produtos.joinToString(
                separator = "\n\n"
            ) { produto ->

                val status = if (produto.sincronizado) {
                    "✓ Sincronizado"
                } else {
                    "⟳ Pendente"
                }

                "${produto.nome} - R$ %.2f\n$status"
                    .format(produto.preco)
            }
        }
    }
}
