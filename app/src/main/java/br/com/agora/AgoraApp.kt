package br.com.agora

import android.app.Application
import br.com.agora.notify.Lembretes

class AgoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lembretes.criarCanal(this)
    }
}
