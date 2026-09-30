package br.com.rastreadorfrota.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
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
    private val colecaoUsuarios = firestore.collection("usuarios")

    /**
     * [incluirMotoristas] só é true para o controlador: o motorista não tem
     * permissão (nem motivo) para baixar os dados dos outros motoristas.
     */
    suspend fun sincronizarTudo(incluirMotoristas: Boolean): ResultadoSincronizacao {
        val v = sincronizarVeiculos()
        if (!incluirMotoristas) return v
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
    // Não há coleção própria: motorista = "usuarios/{uid}" com perfil MOTORISTA.
    // Push envia só o que o controlador gerencia (veículo e status); pull
    // atualiza o cache local com os dados que o próprio motorista cadastrou.

    suspend fun sincronizarMotoristas(): ResultadoSincronizacao {
        val log = mutableListOf<String>()
        var enviados = 0
        var recebidos = 0

        val pendentes = motoristaDao.listarPendentes()
        log.add("[Motoristas] Push: ${pendentes.size} pendente(s).")

        for (motorista in pendentes) {
            // O id do Room só existe neste aparelho; na nuvem o vínculo é
            // pelo remoteId do veículo. Se o veículo ainda não subiu, o
            // motorista fica pendente pra não gravar um vínculo vazio.
            val veiculoRemoteId = motorista.veiculoId?.let { veiculoDao.buscarPorId(it)?.remoteId }
            if (motorista.veiculoId != null && veiculoRemoteId == null) {
                log.add("  → '${motorista.nome}' aguardando o veículo sincronizar.")
                continue
            }

            colecaoUsuarios.document(motorista.uid)
                .update(
                    mapOf(
                        Usuario.CAMPO_VEICULO_REMOTE_ID to veiculoRemoteId,
                        Usuario.CAMPO_ATIVO to motorista.ativo
                    )
                )
                .await()
            motoristaDao.marcarSincronizado(motorista.uid, motorista.veiculoId, motorista.ativo)
            log.add("  → '${motorista.nome}' atualizado no Firestore.")
            enviados++
        }

        val snapshot = colecaoUsuarios.whereEqualTo("perfil", Perfil.MOTORISTA.name).get().await()
        log.add("[Motoristas] Pull: ${snapshot.size()} motorista(s) na nuvem.")

        val uidsNaNuvem = mutableListOf<String>()
        for (doc in snapshot.documents) {
            val usuario = Usuario.fromDocument(doc) ?: continue
            uidsNaNuvem.add(usuario.uid)

            val local = motoristaDao.buscarPorUid(usuario.uid)
            // Alteração do controlador ainda não enviada: a local prevalece.
            if (local != null && !local.sincronizado) continue

            val remoto = MotoristaEntity(
                uid = usuario.uid,
                nome = usuario.nome,
                email = usuario.email,
                telefone = usuario.telefone,
                cnh = usuario.cnh,
                categoriaCnh = usuario.categoriaCnh,
                validadeCnh = usuario.validadeCnh,
                veiculoId = usuario.veiculoRemoteId?.let { veiculoDao.buscarPorRemoteId(it)?.id },
                ativo = usuario.ativo,
                sincronizado = true
            )
            if (remoto != local) {
                motoristaDao.salvar(remoto)
                log.add(
                    if (local == null) "  ← '${usuario.nome}' recebido do Firestore."
                    else "  ← '${usuario.nome}' atualizado a partir da nuvem."
                )
                recebidos++
            }
        }

        val removidos = motoristaDao.removerAusentes(uidsNaNuvem)
        if (removidos > 0) log.add("  ← $removidos motorista(s) removido(s) do cache local.")

        return ResultadoSincronizacao(enviados, recebidos + removidos, 0, log)
    }
}
