package br.com.agora

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.agora.ui.Acoes
import br.com.agora.ui.AgoraViewModel
import br.com.agora.ui.Passo
import br.com.agora.ui.TelaAgora
import br.com.agora.ui.theme.TemaAgora

class MainActivity : ComponentActivity() {

    private val pedirAviso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedirPermissaoDeAviso()

        setContent {
            TemaAgora {
                val vm: AgoraViewModel = viewModel()
                val estado by vm.estado.collectAsStateWithLifecycle()
                TelaAgora(
                    estado = estado,
                    acoes = Acoes(
                        escolherEnergia = vm::escolherEnergia,
                        escolherTempo = vm::escolherTempo,
                        trocar = vm::trocar,
                        comecar = vm::comecar,
                        comecarPausa = vm::comecarPausa,
                        concluir = vm::concluir,
                        parar = vm::parar,
                        proxima = vm::proxima,
                        maisCincoMinutos = vm::maisCincoMinutos,
                        ampulhetaPronta = vm::ampulhetaPronta,
                        limparAviso = vm::limparAviso,
                        abrirFolha = vm::abrirFolha,
                        fecharFolha = vm::fecharFolha,
                        mudarTitulo = vm::mudarTitulo,
                        mudarMinutos = vm::mudarMinutos,
                        mudarEnergia = vm::mudarEnergia,
                        mudarObs = vm::mudarObs,
                        pedirData = vm::pedirData,
                        responderSim = vm::responderSim,
                        responderNao = vm::responderNao,
                        voltarPasso = vm::voltarPasso,
                        escolherDia = { dia -> vm.escolherData(Passo.DIA, dia) },
                        escolherMes = { mes -> vm.escolherData(Passo.MES, mes) },
                        escolherAno = { ano -> vm.escolherData(Passo.ANO, ano) },
                        escolherHora = { hora -> vm.escolherData(Passo.HORARIO, hora) },
                        escolherMinuto = vm::escolherMinuto,
                        horarioPronto = vm::horarioPronto,
                        guardarComData = vm::guardarComData
                    )
                )
            }
        }
    }

    /** No Android 13 e acima, o aviso de lembrete precisa da permissão da pessoa. */
    private fun pedirPermissaoDeAviso() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val concedida = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!concedida) runCatching { pedirAviso.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
