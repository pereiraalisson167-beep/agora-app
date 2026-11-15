package br.com.agora.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import br.com.agora.domain.Tarefa

/**
 * Lembretes das tarefas com hora marcada.
 *
 * Quando o celular permite alarme exato, o lembrete cai na hora. Quando não permite,
 * o sistema entrega um pouco depois, em vez de o app falhar.
 */
object Lembretes {

    const val CANAL = "lembretes"
    const val EXTRA_TITULO = "titulo"
    const val EXTRA_OBS = "obs"
    const val EXTRA_ID = "id"

    fun criarCanal(context: Context) {
        val gerenciador = context.getSystemService(NotificationManager::class.java) ?: return
        val canal = NotificationChannel(
            CANAL,
            "Lembretes de tarefas",
            NotificationManager.IMPORTANCE_HIGH
        )
        canal.description = "Avisa quando chega a hora marcada de uma tarefa."
        gerenciador.createNotificationChannel(canal)
    }

    fun marcar(context: Context, tarefa: Tarefa) {
        val quando = tarefa.em ?: return
        if (quando <= System.currentTimeMillis()) return
        val alarmes = context.getSystemService(AlarmManager::class.java) ?: return
        val aviso = intencao(context, tarefa)
        runCatching {
            val exato = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmes.canScheduleExactAlarms()
            if (exato) alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, aviso)
            else alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, aviso)
        }.onFailure { erro ->
            Log.w("Agora", "Não deu para marcar o lembrete da tarefa ${tarefa.id}", erro)
            runCatching { alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, aviso) }
        }
    }

    fun desmarcar(context: Context, id: Long) {
        val alarmes = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { alarmes.cancel(intencaoVazia(context, id)) }
            .onFailure { erro -> Log.w("Agora", "Não deu para desmarcar o lembrete $id", erro) }
    }

    private fun intencao(context: Context, tarefa: Tarefa): PendingIntent {
        val intent = Intent(context, LembreteReceiver::class.java).apply {
            putExtra(EXTRA_TITULO, tarefa.titulo)
            putExtra(EXTRA_OBS, tarefa.obs)
            putExtra(EXTRA_ID, tarefa.id)
        }
        return PendingIntent.getBroadcast(
            context,
            tarefa.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun intencaoVazia(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, LembreteReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}
