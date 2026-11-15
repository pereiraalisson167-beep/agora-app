package br.com.agora.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TarefaDao {
    @Query("SELECT * FROM tarefas ORDER BY criada_em ASC")
    fun todas(): Flow<List<TarefaEntity>>

    @Query("SELECT * FROM tarefas WHERE em IS NOT NULL")
    suspend fun comData(): List<TarefaEntity>

    @Insert
    suspend fun inserir(tarefa: TarefaEntity): Long

    @Update
    suspend fun atualizar(tarefa: TarefaEntity)

    @Delete
    suspend fun apagar(tarefa: TarefaEntity)

    @Query("DELETE FROM tarefas WHERE id = :id")
    suspend fun apagarPorId(id: Long)

    @Insert
    suspend fun registrarConclusao(conclusao: ConclusaoEntity)

    @Query("SELECT COUNT(*) FROM concluidas WHERE quando >= :desde")
    fun concluidasDesde(desde: Long): Flow<Int>
}
