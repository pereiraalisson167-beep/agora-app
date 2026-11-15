package br.com.agora.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import br.com.agora.domain.Tarefa

/** A tarefa como ela fica guardada no celular. */
@Entity(tableName = "tarefas")
data class TarefaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "minutos") val minutos: Int,
    @ColumnInfo(name = "energia") val energia: Int,
    @ColumnInfo(name = "criada_em") val criadaEm: Long,
    @ColumnInfo(name = "em") val em: Long? = null,
    @ColumnInfo(name = "obs") val obs: String = ""
) {
    fun paraDominio(): Tarefa =
        Tarefa(id = id, titulo = titulo, minutos = minutos, energia = energia, criadaEm = criadaEm, em = em, obs = obs)
}

fun Tarefa.paraBanco(): TarefaEntity =
    TarefaEntity(id = id, titulo = titulo, minutos = minutos, energia = energia, criadaEm = criadaEm, em = em, obs = obs)

/** Cada tarefa concluída, para contar quantas saíram no dia. */
@Entity(tableName = "concluidas")
data class ConclusaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "titulo") val titulo: String,
    @ColumnInfo(name = "quando") val quando: Long
)
