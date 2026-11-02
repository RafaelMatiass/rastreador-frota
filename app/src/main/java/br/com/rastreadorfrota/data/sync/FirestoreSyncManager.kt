package br.com.rastreadorfrota.data.sync

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import br.com.rastreadorfrota.auth.Perfil
import br.com.rastreadorfrota.auth.Usuario
import br.com.rastreadorfrota.data.local.dao.AlertaDao
import br.com.rastreadorfrota.data.local.dao.CercaDao
import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.SituacaoVeiculo
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity
import br.com.rastreadorfrota.data.repository.RegrasFrota
import kotlinx.coroutines.tasks.await

private const val CAMPO_UPDATED_AT = "updatedAt"
private const val CAMPO_REMOVIDO = "removido"

/** Alertas do aparelho dos motoristas que o gestor baixa: só os da última semana. */
private const val JANELA_ALERTAS_MS = 7L * 24 * 60 * 60 * 1000

data class ResultadoSincronizacao(
    val enviados: Int,
    val recebidos: Int,
    val conflitosResolvidos: Int,
    val log: List<String>
) {
    operator fun plus(outro: ResultadoSincronizacao) = ResultadoSincronizacao(
        enviados + outro.enviados,
        recebidos + outro.recebidos,
        conflitosResolvidos + outro.conflitosResolvidos,
        log + outro.log
    )

    companion object {
        val VAZIO = ResultadoSincronizacao(0, 0, 0, emptyList())
    }
}

class FirestoreSyncManager(
    private val veiculoDao: VeiculoDao,
    private val motoristaDao: MotoristaDao,
    private val cercaDao: CercaDao,
    private val alertaDao: AlertaDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val colecaoVeiculos = firestore.collection("veiculos")
    private val colecaoUsuarios = firestore.collection("usuarios")
    private val colecaoCercas = firestore.collection("cercas")
    private val colecaoAlertas = firestore.collection("alertas")

    /** Chamado para cada alerta novo vindo do aparelho de um motorista (o gestor é notificado). */
    var aoReceberAlerta: ((AlertaEntity) -> Unit)? = null

    /**
     * O que cada perfil sincroniza:
     * - Controlador: veículos, motoristas, cercas (envia e recebe) e baixa os alertas dos motoristas.
     * - Motorista: veículos, baixa as cercas (para o geofencing do aparelho) e envia os alertas dele.
     *   Ele não tem permissão (nem motivo) para baixar os dados dos outros motoristas.
     */
    suspend fun sincronizarTudo(perfil: Perfil): ResultadoSincronizacao {
        var resultado = sincronizarVeiculos()
        resultado += when (perfil) {
            Perfil.CONTROLADOR -> sincronizarMotoristas() + sincronizarCercas(enviar = true) + receberAlertas()
            Perfil.MOTORISTA -> sincronizarCercas(enviar = false) + enviarAlertas()
        }
        return resultado
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
                "situacao" to veiculo.situacao,
                "cadastradoPorUid" to veiculo.cadastradoPorUid,
                "odometroKm" to veiculo.odometroKm,
                "ultimaVistoriaEm" to veiculo.ultimaVistoriaEm,
                "observacoes" to veiculo.observacoes,
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
            val remoto = VeiculoEntity(
                placa = placa,
                modelo = doc.getString("modelo") ?: "",
                tipo = doc.getString("tipo") ?: "OUTRO",
                capacidadeCargaKg = doc.getDouble("capacidadeCargaKg"),
                ativo = doc.getBoolean("ativo") ?: true,
                situacao = SituacaoVeiculo.de(doc.getString("situacao")).name,
                cadastradoPorUid = doc.getString("cadastradoPorUid"),
                odometroKm = doc.getLong("odometroKm")?.toInt(),
                ultimaVistoriaEm = doc.getLong("ultimaVistoriaEm"),
                observacoes = doc.getString("observacoes"),
                remoteId = remoteId,
                updatedAt = updatedAtRemoto,
                sincronizado = true
            )

            if (local == null) {
                veiculoDao.inserir(remoto)
                log.add("  ← '$placa' recebido do Firestore (novo neste aparelho).")
                recebidos++
            } else if (local.sincronizado && updatedAtRemoto > local.updatedAt) {
                // A foto é só do aparelho: nunca vem da nuvem, então a local é mantida.
                veiculoDao.atualizar(remoto.copy(id = local.id, fotoLocalPath = local.fotoLocalPath))
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

        // Um veículo, um motorista: dados antigos com o mesmo veículo em dois motoristas são
        // corrigidos aqui e a correção sobe na próxima sincronização.
        RegrasFrota.duplicadosParaSoltar(motoristaDao.listarTodos()).forEach {
            motoristaDao.definirVeiculo(it.uid, null)
            log.add("  ⚠ '${it.nome}' estava com um veículo de outro motorista e ficou sem veículo.")
        }

        return ResultadoSincronizacao(enviados, recebidos + removidos, 0, log)
    }

    // ---------------- CERCAS (geofencing) ----------------

    /** [enviar]: só o controlador cria e altera cercas; o motorista apenas recebe. */
    suspend fun sincronizarCercas(enviar: Boolean): ResultadoSincronizacao {
        val log = mutableListOf<String>()
        var enviados = 0
        var recebidos = 0

        if (enviar) {
            val pendentes = cercaDao.listarPendentes()
            log.add("[Cercas] Push: ${pendentes.size} pendente(s).")
            for (cerca in pendentes) {
                if (cerca.deletedLocally) {
                    cerca.remoteId?.let {
                        colecaoCercas.document(it)
                            .set(mapOf(CAMPO_REMOVIDO to true, CAMPO_UPDATED_AT to cerca.updatedAt), SetOptions.merge())
                            .await()
                    }
                    cercaDao.excluirDefinitivo(cerca.id)
                    enviados++
                    continue
                }
                val dados = mapOf(
                    "nome" to cerca.nome,
                    "latitude" to cerca.latitude,
                    "longitude" to cerca.longitude,
                    "raioM" to cerca.raioM,
                    "alvo" to cerca.alvo,
                    "alvoValor" to cerca.alvoValor,
                    "notificarEntrada" to cerca.notificarEntrada,
                    "notificarSaida" to cerca.notificarSaida,
                    "ativa" to cerca.ativa,
                    CAMPO_UPDATED_AT to cerca.updatedAt,
                    CAMPO_REMOVIDO to false
                )
                if (cerca.remoteId == null) {
                    val ref = colecaoCercas.add(dados).await()
                    cercaDao.atualizar(cerca.copy(remoteId = ref.id, sincronizado = true))
                } else {
                    colecaoCercas.document(cerca.remoteId).set(dados, SetOptions.merge()).await()
                    cercaDao.atualizar(cerca.copy(sincronizado = true))
                }
                log.add("  → Cerca '${cerca.nome}' enviada.")
                enviados++
            }
        }

        val snapshot = colecaoCercas.get().await()
        log.add("[Cercas] Pull: ${snapshot.size()} cerca(s) na nuvem.")
        for (doc in snapshot.documents) {
            val local = cercaDao.buscarPorRemoteId(doc.id)
            val updatedAtRemoto = doc.getLong(CAMPO_UPDATED_AT) ?: 0L
            if (doc.getBoolean(CAMPO_REMOVIDO) == true) {
                if (local != null && local.sincronizado) {
                    cercaDao.excluirDefinitivo(local.id)
                    recebidos++
                }
                continue
            }
            val remota = CercaEntity(
                nome = doc.getString("nome") ?: continue,
                latitude = doc.getDouble("latitude") ?: continue,
                longitude = doc.getDouble("longitude") ?: continue,
                raioM = doc.getDouble("raioM") ?: 200.0,
                alvo = doc.getString("alvo") ?: "TODOS",
                alvoValor = doc.getString("alvoValor") ?: "",
                notificarEntrada = doc.getBoolean("notificarEntrada") ?: true,
                notificarSaida = doc.getBoolean("notificarSaida") ?: true,
                ativa = doc.getBoolean("ativa") ?: true,
                remoteId = doc.id,
                updatedAt = updatedAtRemoto,
                sincronizado = true
            )
            if (local == null) {
                cercaDao.inserir(remota)
                log.add("  ← Cerca '${remota.nome}' recebida.")
                recebidos++
            } else if (local.sincronizado && updatedAtRemoto > local.updatedAt) {
                cercaDao.atualizar(remota.copy(id = local.id))
                recebidos++
            }
        }
        return ResultadoSincronizacao(enviados, recebidos, 0, log)
    }

    // ---------------- ALERTAS ----------------
    // Só os alertas do GPS real do motorista vão para a nuvem. O id é determinístico,
    // então reenviar (ou receber de novo) o mesmo alerta não duplica.

    private suspend fun enviarAlertas(): ResultadoSincronizacao {
        val pendentes = alertaDao.listarPendentes()
        for (alerta in pendentes) {
            colecaoAlertas.document(alerta.id).set(
                mapOf(
                    "cercaNome" to alerta.cercaNome,
                    "placa" to alerta.placa,
                    "tipo" to alerta.tipo,
                    "origem" to alerta.origem,
                    "instante" to alerta.instante,
                    "motoristaNome" to alerta.motoristaNome,
                    "uid" to alerta.uid
                )
            ).await()
            alertaDao.marcarSincronizado(alerta.id)
        }
        return ResultadoSincronizacao(pendentes.size, 0, 0, listOf("[Alertas] Push: ${pendentes.size} alerta(s) do aparelho."))
    }

    private suspend fun receberAlertas(): ResultadoSincronizacao {
        val desde = System.currentTimeMillis() - JANELA_ALERTAS_MS
        val snapshot = colecaoAlertas.whereGreaterThan("instante", desde).get().await()
        var novos = 0
        for (doc in snapshot.documents) {
            val alerta = AlertaEntity(
                id = doc.id,
                cercaNome = doc.getString("cercaNome") ?: continue,
                placa = doc.getString("placa") ?: continue,
                tipo = doc.getString("tipo") ?: continue,
                origem = doc.getString("origem") ?: "APARELHO",
                instante = doc.getLong("instante") ?: continue,
                motoristaNome = doc.getString("motoristaNome"),
                uid = doc.getString("uid"),
                sincronizado = true
            )
            if (alertaDao.inserir(alerta) != -1L) {
                novos++
                aoReceberAlerta?.invoke(alerta)
            }
        }
        return ResultadoSincronizacao(0, novos, 0, listOf("[Alertas] Pull: $novos alerta(s) novo(s) dos motoristas."))
    }
}
