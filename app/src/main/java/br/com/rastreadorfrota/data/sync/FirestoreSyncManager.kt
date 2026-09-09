package br.com.rastreadorfrota.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import kotlinx.coroutines.tasks.await

private const val CAMPO_UPDATED_AT = "updatedAt"
private const val CAMPO_REMOVIDO = "removido"

data class ResultadoSincronizacao(
    val enviados: Int,
    val recebidos: Int,
    val conflitosResolvidos: Int,
    val log: List<String>
)

class FirestoreSyncManager(
    private val veiculoDao: VeiculoDao,
    private val motoristaDao: MotoristaDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecaoVeiculos = firestore.collection("veiculos")
    private val colecaoMotoristas = firestore.collection("motoristas")

    suspend fun sincronizarTudo(): ResultadoSincronizacao {
        val v = sincronizarVeiculos()
        val m = sincronizarMotoristas()
        return ResultadoSincronizacao(
            enviados = v.enviados + m.enviados,
            recebidos = v.recebidos + m.recebidos,
            conflitosResolvidos = v.conflitosResolvidos + m.conflitosResolvidos,
            log = v.log + m.log
        )
    }

    // ---------------- VEÍCULOS ----------------

    suspend fun sincronizarVeiculos(): ResultadoSincronizacao {
        val log = mutableListOf<String>()
        var enviados = 0
        var recebidos = 0
        var conflitos = 0

        val pendentes = veiculoDao.listarPendentes()
        log.add("[Veículos] Push: ${pendentes.size} pendente(s).")

        for (veiculo in pendentes) {
            if (veiculo.deletedLocally) {
                if (veiculo.remoteId != null) {
                    colecaoVeiculos.document(veiculo.remoteId)
                        .set(mapOf(CAMPO_REMOVIDO to true, CAMPO_UPDATED_AT to veiculo.updatedAt), SetOptions.merge())
                        .await()
                    log.add("  → Exclusão de '${veiculo.placa}' propagada ao Firestore.")
                }
                veiculoDao.excluirDefinitivo(veiculo.id)
                enviados++
                continue
            }

            val dados = mapOf(
                "placa" to veiculo.placa,
                "modelo" to veiculo.modelo,
                "tipo" to veiculo.tipo,
                "capacidadeCargaKg" to veiculo.capacidadeCargaKg,
                "ativo" to veiculo.ativo,
                CAMPO_UPDATED_AT to veiculo.updatedAt,
                CAMPO_REMOVIDO to false
            )

            if (veiculo.remoteId == null) {
                val ref = colecaoVeiculos.add(dados).await()
                veiculoDao.atualizar(veiculo.copy(remoteId = ref.id, sincronizado = true))
                log.add("  → '${veiculo.placa}' criado no Firestore (${ref.id}).")
            } else {
                colecaoVeiculos.document(veiculo.remoteId).set(dados, SetOptions.merge()).await()
                veiculoDao.atualizar(veiculo.copy(sincronizado = true))
                log.add("  → '${veiculo.placa}' atualizado no Firestore.")
            }
            enviados++
        }

        val snapshot = colecaoVeiculos.get().await()
        log.add("[Veículos] Pull: ${snapshot.size()} documento(s) na nuvem.")

        for (doc in snapshot.documents) {
            val remoteId = doc.id
            val removido = doc.getBoolean(CAMPO_REMOVIDO) ?: false
            val updatedAtRemoto = doc.getLong(CAMPO_UPDATED_AT) ?: 0L
            val local = veiculoDao.buscarPorRemoteId(remoteId)

            if (removido) {
                if (local != null && local.sincronizado) {
                    veiculoDao.excluirDefinitivo(local.id)
                    log.add("  ← '${local.placa}' removido localmente (excluído em outro dispositivo).")
                    recebidos++
                }
                continue
            }

            val placa = doc.getString("placa") ?: continue
            val modelo = doc.getString("modelo") ?: ""
            val tipo = doc.getString("tipo") ?: "OUTRO"
            val capacidade = doc.getDouble("capacidadeCargaKg")
            val ativo = doc.getBoolean("ativo") ?: true

            if (local == null) {
                veiculoDao.inserir(
                    VeiculoEntity(
                        placa = placa,
                        modelo = modelo,
                        tipo = tipo,
                        capacidadeCargaKg = capacidade,
                        ativo = ativo,
                        remoteId = remoteId,
                        updatedAt = updatedAtRemoto,
                        sincronizado = true
                    )
                )
                log.add("  ← '$placa' recebido do Firestore (novo neste aparelho).")
                recebidos++
            } else if (local.sincronizado && updatedAtRemoto > local.updatedAt) {
                veiculoDao.atualizar(
                    local.copy(
                        placa = placa,
                        modelo = modelo,
                        tipo = tipo,
                        capacidadeCargaKg = capacidade,
                        ativo = ativo,
                        updatedAt = updatedAtRemoto
                    )
                )
                log.add("  ← '$placa' atualizado a partir da nuvem.")
                conflitos++
            }
        }

        return ResultadoSincronizacao(enviados, recebidos, conflitos, log)
    }

    // ---------------- MOTORISTAS ----------------

    suspend fun sincronizarMotoristas(): ResultadoSincronizacao {
        val log = mutableListOf<String>()
        var enviados = 0
        var recebidos = 0
        var conflitos = 0

        val pendentes = motoristaDao.listarPendentes()
        log.add("[Motoristas] Push: ${pendentes.size} pendente(s).")

        for (motorista in pendentes) {
            if (motorista.deletedLocally) {
                if (motorista.remoteId != null) {
                    colecaoMotoristas.document(motorista.remoteId)
                        .set(mapOf(CAMPO_REMOVIDO to true, CAMPO_UPDATED_AT to motorista.updatedAt), SetOptions.merge())
                        .await()
                    log.add("  → Exclusão de '${motorista.nome}' propagada ao Firestore.")
                }
                motoristaDao.excluirDefinitivo(motorista.id)
                enviados++
                continue
            }

            val dados = mapOf(
                "nome" to motorista.nome,
                "cnh" to motorista.cnh,
                "telefone" to motorista.telefone,
                "email" to motorista.email,
                "veiculoId" to motorista.veiculoId,
                "ativo" to motorista.ativo,
                CAMPO_UPDATED_AT to motorista.updatedAt,
                CAMPO_REMOVIDO to false
            )

            if (motorista.remoteId == null) {
                val ref = colecaoMotoristas.add(dados).await()
                motoristaDao.atualizar(motorista.copy(remoteId = ref.id, sincronizado = true))
                log.add("  → '${motorista.nome}' criado no Firestore (${ref.id}).")
            } else {
                colecaoMotoristas.document(motorista.remoteId).set(dados, SetOptions.merge()).await()
                motoristaDao.atualizar(motorista.copy(sincronizado = true))
                log.add("  → '${motorista.nome}' atualizado no Firestore.")
            }
            enviados++
        }

        val snapshot = colecaoMotoristas.get().await()
        log.add("[Motoristas] Pull: ${snapshot.size()} documento(s) na nuvem.")

        for (doc in snapshot.documents) {
            val remoteId = doc.id
            val removido = doc.getBoolean(CAMPO_REMOVIDO) ?: false
            val updatedAtRemoto = doc.getLong(CAMPO_UPDATED_AT) ?: 0L
            val local = motoristaDao.buscarPorRemoteId(remoteId)

            if (removido) {
                if (local != null && local.sincronizado) {
                    motoristaDao.excluirDefinitivo(local.id)
                    log.add("  ← '${local.nome}' removido localmente (excluído em outro dispositivo).")
                    recebidos++
                }
                continue
            }

            val nome = doc.getString("nome") ?: continue
            val cnh = doc.getString("cnh") ?: ""
            val telefone = doc.getString("telefone") ?: ""
            val email = doc.getString("email") ?: ""
            val veiculoId = doc.getLong("veiculoId")
            val ativo = doc.getBoolean("ativo") ?: true

            if (local == null) {
                motoristaDao.inserir(
                    MotoristaEntity(
                        nome = nome,
                        cnh = cnh,
                        telefone = telefone,
                        email = email,
                        veiculoId = veiculoId,
                        ativo = ativo,
                        remoteId = remoteId,
                        updatedAt = updatedAtRemoto,
                        sincronizado = true
                    )
                )
                log.add("  ← '$nome' recebido do Firestore (novo neste aparelho).")
                recebidos++
            } else if (local.sincronizado && updatedAtRemoto > local.updatedAt) {
                motoristaDao.atualizar(
                    local.copy(
                        nome = nome,
                        cnh = cnh,
                        telefone = telefone,
                        email = email,
                        veiculoId = veiculoId,
                        ativo = ativo,
                        updatedAt = updatedAtRemoto
                    )
                )
                log.add("  ← '$nome' atualizado a partir da nuvem.")
                conflitos++
            }
        }

        return ResultadoSincronizacao(enviados, recebidos, conflitos, log)
    }
}