package br.com.agora.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import br.com.agora.data.Repositorio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Ao reiniciar o celular, o Android apaga os alarmes marcados. Aqui eles são remarcados
 * a partir das tarefas com data que ainda estão guardadas.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext
        val resultado = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Repositorio(app).remarcarLembretes()
            } catch (erro: Throwable) {
                Log.w("Agora", "Não deu para remarcar os lembretes depois de reiniciar", erro)
            } finally {
                resultado.finish()
            }
        }
    }
}
