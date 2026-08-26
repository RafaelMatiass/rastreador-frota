package br.com.rastreadorfrota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity

@Database(
    entities = [VeiculoEntity::class, MotoristaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun veiculoDao(): VeiculoDao
    abstract fun motoristaDao(): MotoristaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rastreadorfrota.db"
                )
                    // Ideal seria escrever Migrations quando o schema mudar;
                    // destructive é aceitável agora pois ainda estamos na fase 1.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}