package br.com.agora.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * O banco do app. A estrutura de cada versão fica guardada em app/schemas, para que uma
 * versão futura com campos novos possa atualizar o banco sem apagar as tarefas.
 */
@Database(
    entities = [TarefaEntity::class, ConclusaoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tarefaDao(): TarefaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        fun obter(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agora.db"
                ).build().also { instancia = it }
            }
    }
}
