package br.com.agora.notify

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.agora.MainActivity
import br.com.agora.R

/** Mostra a notificação na hora marcada da tarefa. */
class LembreteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo = intent.getStringExtra(Lembretes.EXTRA_TITULO) ?: return
        val obs = intent.getStringExtra(Lembretes.EXTRA_OBS).orEmpty()
        val id = intent.getLongExtra(Lembretes.EXTRA_ID, 0L)

        Lembretes.criarCanal(context)
        val abrir = PendingIntent.getActivity(
            context,
            id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val aviso = Notification.Builder(context, Lembretes.CANAL)
            .setSmallIcon(R.drawable.ic_sino)
            .setContentTitle("Chegou a hora: $titulo")
            .setContentText(obs.ifBlank { "Toque para abrir o Agora." })
            .setStyle(Notification.BigTextStyle().bigText(obs.ifBlank { "Toque para abrir o Agora." }))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()

        runCatching {
            context.getSystemService(NotificationManager::class.java)?.notify(id.toInt(), aviso)
        }.onFailure { erro ->
            Log.w("Agora", "Não deu para mostrar o lembrete da tarefa $id", erro)
        }
    }
}
