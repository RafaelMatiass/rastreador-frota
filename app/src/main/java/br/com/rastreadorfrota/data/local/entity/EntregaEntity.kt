package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SituacaoEntrega(val label: String) {
    /** Na fila: começa quando o veículo terminar a entrega anterior. */
    AGENDADA("Na fila"),
    EM_ANDAMENTO("Em andamento"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada")
}

/**
 * Entrega atribuída pelo gestor a um veículo: CD -> cliente -> CD.
 *
 * As rotas são geradas UMA vez no OSRM, na criação, e guardadas aqui como polyline codificada.
 * Junto com [inicioEm] (sincronizado), isso basta para qualquer aparelho calcular a mesma
 * posição do veículo a qualquer momento, e também para reconstruir o histórico depois.
 *
 * - [rotaIda]/[rotaVolta]: rota PLANEJADA (o que o gestor espera).
 * - [rotaIdaExecutada]: o que o veículo de fato percorre na ida. Igual à planejada, ou com um
 *   desvio quando [comDesvio] (demonstração da detecção de desvio de rota).
 *
 * Mesmo padrão de sincronização das cercas: grava no Room e sobe para "entregas" no Firestore.
 * O veículo é identificado pela placa, que é a mesma em todos os aparelhos.
 *
 * Fila: várias entregas do mesmo veículo são encadeadas (ver `encadearEntrega`). A nova sai do
 * cliente da anterior ([origemNome]) e a volta ao CD da anterior é cortada: o [fimPrevistoEm]
 * dela passa a ser o início da próxima. Só a última da fila volta ao CD.
 */
@Entity(tableName = "entregas")
data class EntregaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placa: String,
    val clienteNome: String,
    val endereco: String,
    val latitude: Double,
    val longitude: Double,
    val rotaIda: String,
    val rotaVolta: String,
    val rotaIdaExecutada: String,
    val kmhIda: Int,
    val kmhVolta: Int,
    val comDesvio: Boolean = false,
    /** De onde a entrega sai: o CD ou o cliente da entrega anterior da fila. */
    val origemNome: String = "CD Morada do Sol",
    val inicioEm: Long,
    val fimPrevistoEm: Long,
    val canceladaEm: Long? = null,
    /**
     * Cancelada no caminho, sem outra entrega na fila: rota de volta ao CD a partir de onde o
     * veículo estava no cancelamento (ver `viagemDaEntrega`). Null quando não houve retorno.
     */
    val rotaRetorno: String? = null,
    val motoristaNome: String? = null,
    val criadaPorUid: String? = null,
    val criadaEm: Long = System.currentTimeMillis(),
    val remoteId: String? = null,
    val sincronizado: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedLocally: Boolean = false
) {
    fun situacao(agora: Long = System.currentTimeMillis()): SituacaoEntrega = when {
        canceladaEm != null -> SituacaoEntrega.CANCELADA
        agora < inicioEm -> SituacaoEntrega.AGENDADA
        agora < fimPrevistoEm -> SituacaoEntrega.EM_ANDAMENTO
        else -> SituacaoEntrega.CONCLUIDA
    }

    /** Cancelada, mas o veículo ainda está voltando ao CD a partir do ponto do cancelamento. */
    fun voltandoAposCancelar(agora: Long = System.currentTimeMillis()): Boolean =
        canceladaEm != null && rotaRetorno != null && agora >= inicioEm && agora < fimPrevistoEm

    /** O veículo está na rua por causa desta entrega (em andamento ou voltando após cancelar). */
    fun naRua(agora: Long = System.currentTimeMillis()): Boolean =
        situacao(agora) == SituacaoEntrega.EM_ANDAMENTO || voltandoAposCancelar(agora)

    /** Ocupa o veículo: ainda não terminou (incluindo a volta ao CD depois de um cancelamento). */
    fun ativa(agora: Long = System.currentTimeMillis()): Boolean =
        situacao(agora) == SituacaoEntrega.AGENDADA || naRua(agora)
}
