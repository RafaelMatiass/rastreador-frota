package com.rastreadorfrota.poc.sqlite.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rastreadorfrota.poc.sqlite.data.dao.MotoristaDao
import com.rastreadorfrota.poc.sqlite.data.dao.NegocianteDao
import com.rastreadorfrota.poc.sqlite.data.dao.OcorrenciaEntregaDao
import com.rastreadorfrota.poc.sqlite.data.dao.ProdutoDao
import com.rastreadorfrota.poc.sqlite.data.dao.VeiculoDao
import com.rastreadorfrota.poc.sqlite.data.entity.Motorista
import com.rastreadorfrota.poc.sqlite.data.entity.Negociante
import com.rastreadorfrota.poc.sqlite.data.entity.OcorrenciaEntrega
import com.rastreadorfrota.poc.sqlite.data.entity.Produto
import com.rastreadorfrota.poc.sqlite.data.entity.Veiculo

@Database(
    entities = [
        Produto::class,
        Negociante::class,
        Motorista::class,
        Veiculo::class,
        OcorrenciaEntrega::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun produtoDao(): ProdutoDao
    abstract fun negocianteDao(): NegocianteDao
    abstract fun motoristaDao(): MotoristaDao
    abstract fun veiculoDao(): VeiculoDao
    abstract fun ocorrenciaEntregaDao(): OcorrenciaEntregaDao

    companion object {
        // Volatile garante que a instância seja visível imediatamente pra
        // todas as threads assim que ela é criada — evita corrida entre
        // duas threads criando dois bancos ao mesmo tempo.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "poc_sqlite_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
