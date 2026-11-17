package br.com.rastreadorfrota.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.rastreadorfrota.data.local.dao.AlertaDao
import br.com.rastreadorfrota.data.local.dao.CercaDao
import br.com.rastreadorfrota.data.local.dao.EntregaDao
import br.com.rastreadorfrota.data.local.dao.MotoristaDao
import br.com.rastreadorfrota.data.local.dao.VeiculoDao
import br.com.rastreadorfrota.data.local.entity.AlertaEntity
import br.com.rastreadorfrota.data.local.entity.CercaEntity
import br.com.rastreadorfrota.data.local.entity.EntregaEntity
import br.com.rastreadorfrota.data.local.entity.MotoristaEntity
import br.com.rastreadorfrota.data.local.entity.VeiculoEntity

@Database(
    entities = [VeiculoEntity::class, MotoristaEntity::class, CercaEntity::class, AlertaEntity::class, EntregaEntity::class],
    version = 7, // v6: entregas (rota planejada, desvio, histórico); v7: fila de entregas (origem)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun veiculoDao(): VeiculoDao
    abstract fun motoristaDao(): MotoristaDao
    abstract fun cercaDao(): CercaDao
    abstract fun alertaDao(): AlertaDao
    abstract fun entregaDao(): EntregaDao

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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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

        // v5: colunas novas em "veiculos" (os já cadastrados continuam ATIVOS) e as tabelas
        // do geofencing. Nada é apagado: quem atualiza o app mantém a frota.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE veiculos ADD COLUMN situacao TEXT NOT NULL DEFAULT 'ATIVO'")
                database.execSQL("ALTER TABLE veiculos ADD COLUMN cadastradoPorUid TEXT")
                database.execSQL("ALTER TABLE veiculos ADD COLUMN odometroKm INTEGER")
                database.execSQL("ALTER TABLE veiculos ADD COLUMN ultimaVistoriaEm INTEGER")
                database.execSQL("ALTER TABLE veiculos ADD COLUMN observacoes TEXT")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cercas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`nome` TEXT NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, " +
                        "`raioM` REAL NOT NULL, `alvo` TEXT NOT NULL, `alvoValor` TEXT NOT NULL, " +
                        "`notificarEntrada` INTEGER NOT NULL, `notificarSaida` INTEGER NOT NULL, " +
                        "`ativa` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, `remoteId` TEXT, " +
                        "`updatedAt` INTEGER NOT NULL, `deletedLocally` INTEGER NOT NULL)"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alertas` (`id` TEXT NOT NULL, `cercaNome` TEXT NOT NULL, " +
                        "`placa` TEXT NOT NULL, `tipo` TEXT NOT NULL, `origem` TEXT NOT NULL, " +
                        "`instante` INTEGER NOT NULL, `motoristaNome` TEXT, `uid` TEXT, " +
                        "`lido` INTEGER NOT NULL, `sincronizado` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        // v6: tabela de entregas. Nada é apagado: a frota, as cercas e os alertas continuam.
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `entregas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`placa` TEXT NOT NULL, `clienteNome` TEXT NOT NULL, `endereco` TEXT NOT NULL, " +
                        "`latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `rotaIda` TEXT NOT NULL, " +
                        "`rotaVolta` TEXT NOT NULL, `rotaIdaExecutada` TEXT NOT NULL, `kmhIda` INTEGER NOT NULL, " +
                        "`kmhVolta` INTEGER NOT NULL, `comDesvio` INTEGER NOT NULL, `inicioEm` INTEGER NOT NULL, " +
                        "`fimPrevistoEm` INTEGER NOT NULL, `canceladaEm` INTEGER, `motoristaNome` TEXT, " +
                        "`criadaPorUid` TEXT, `criadaEm` INTEGER NOT NULL, `remoteId` TEXT, " +
                        "`sincronizado` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedLocally` INTEGER NOT NULL)"
                )
            }
        }

        // v7: fila de entregas. A entrega guarda de onde sai (CD ou cliente anterior).
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE entregas ADD COLUMN origemNome TEXT NOT NULL DEFAULT 'CD Morada do Sol'")
            }
        }
    }
}
