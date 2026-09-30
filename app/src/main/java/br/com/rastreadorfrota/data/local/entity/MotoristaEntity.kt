package br.com.rastreadorfrota.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cópia local (cache offline) dos usuários com perfil MOTORISTA.
 * O registro "de verdade" é "usuarios/{uid}" no Firestore, criado pelo próprio
 * motorista no "Cadastrar-se". Aqui o controlador só altera [veiculoId] e [ativo].
 */
@Entity(
    tableName = "motoristas",
    foreignKeys = [
        ForeignKey(
            entity = VeiculoEntity::class,
            parentColumns = ["id"],
            childColumns = ["veiculoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("veiculoId")]
)
data class MotoristaEntity(
    @PrimaryKey
    val uid: String,
    val nome: String,
    val email: String,
    val telefone: String,
    val cnh: String,
    val categoriaCnh: String,
    val validadeCnh: String,
    val veiculoId: Long? = null,
    val ativo: Boolean = true,
    val sincronizado: Boolean = true // false = controlador alterou veículo/status e ainda não subiu
)
