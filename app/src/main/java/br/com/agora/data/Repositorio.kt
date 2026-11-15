package br.com.agora.data

import android.content.Context
import br.com.agora.domain.Tarefa
import br.com.agora.notify.Lembretes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

/** Tudo o que o app faz com as tarefas guardadas, em um só lugar. */
class Repositorio(private val context: Context) {

    private val dao = AppDatabase.obter(context).tarefaDao()

    val tarefas: Flow<List<Tarefa>> = dao.todas().map { lista -> lista.map { it.paraDominio() } }

    /**
     * Quantas tarefas saíram hoje. O começo do dia é conferido a cada minuto, para o
     * contador voltar a zero à meia-noite mesmo com o app aberto.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val concluidasHoje: Flow<Int> = flow {
        while (true) {
            emit(inicioDoDia())
            delay(60_000)
        }
    }.distinctUntilChanged().flatMapLatest { desde -> dao.concluidasDesde(desde) }

    suspend fun guardar(tarefa: Tarefa): Long {
        val id = dao.inserir(tarefa.paraBanco())
        if (tarefa.em != null) Lembretes.marcar(context, tarefa.copy(id = id))
        return id
    }

    suspend fun concluir(tarefa: Tarefa, quando: Long) {
        dao.registrarConclusao(ConclusaoEntity(titulo = tarefa.titulo, quando = quando))
        dao.apagarPorId(tarefa.id)
        Lembretes.desmarcar(context, tarefa.id)
    }

    /** Remarca os lembretes de todas as tarefas com data: usado depois que o celular reinicia. */
    suspend fun remarcarLembretes() {
        dao.comData().forEach { Lembretes.marcar(context, it.paraDominio()) }
    }

    private fun inicioDoDia(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
