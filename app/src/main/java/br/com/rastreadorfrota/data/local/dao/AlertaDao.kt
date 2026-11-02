package br.com.rastreadorfrota.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertaDao {
    /** IGNORE: o id é determinístico, então o mesmo evento visto de novo não duplica. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserir(alerta: AlertaEntity): Long

    @Query("SELECT * FROM alertas ORDER BY instante DESC LIMIT :limite")
    fun observarRecentes(limite: Int = 200): Flow<List<AlertaEntity>>

    @Query("SELECT COUNT(*) FROM alertas WHERE lido = 0")
    fun observarNaoLidos(): Flow<Int>

    @Query("SELECT COUNT(*) FROM alertas WHERE instante >= :desde")
    fun observarQuantidadeDesde(desde: Long): Flow<Int>

    @Query("UPDATE alertas SET lido = 1 WHERE lido = 0")
    suspend fun marcarTodosLidos()

    @Query("SELECT * FROM alertas WHERE sincronizado = 0")
    suspend fun listarPendentes(): List<AlertaEntity>

    @Query("SELECT COUNT(*) FROM alertas WHERE sincronizado = 0")
    fun observarQuantidadePendentes(): Flow<Int>

    @Query("UPDATE alertas SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizado(id: String)

    /** Histórico de 30 dias basta: o que é mais antigo sai, pro banco não crescer sem fim. */
    @Query("DELETE FROM alertas WHERE instante < :antesDe AND sincronizado = 1")
    suspend fun removerAntigos(antesDe: Long)
}
