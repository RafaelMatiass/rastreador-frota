package br.com.rastreadorfrota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity

@Database(
    entities = [VeiculoEntity::class, MotoristaEntity::class],
    version = 4, // v4: motoristas viram cache de "usuarios" (chave = uid)
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE veiculos ADD COLUMN fotoLocalPath TEXT")
            }
        }

        // v4: motoristas deixam de ser cadastrados localmente e viram cache de
        // "usuarios" (perfil MOTORISTA), com o uid como chave. Os registros
        // antigos eram outro conceito, então a tabela é recriada vazia e
        // repovoada pela próxima sincronização.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS motoristas")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `motoristas` (`uid` TEXT NOT NULL, `nome` TEXT NOT NULL, " +
                        "`email` TEXT NOT NULL, `telefone` TEXT NOT NULL, `cnh` TEXT NOT NULL, " +
                        "`categoriaCnh` TEXT NOT NULL, `validadeCnh` TEXT NOT NULL, `veiculoId` INTEGER, " +
                        "`ativo` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, PRIMARY KEY(`uid`), " +
                        "FOREIGN KEY(`veiculoId`) REFERENCES `veiculos`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_motoristas_veiculoId` ON `motoristas` (`veiculoId`)")
            }
        }
    }
}