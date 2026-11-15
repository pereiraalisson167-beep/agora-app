package br.com.agora.ui.hourglass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** As peças prontas da ampulheta, no tamanho atual da tela. */
internal class Pecas(
    val tras: Bitmap,
    val frente: Bitmap,
    val areiaTopo: Bitmap,
    val areiaTopoX: Int,
    val areiaTopoY: Int,
    val areiaBase: Bitmap,
    val areiaBaseX: Int,
    val areiaBaseY: Int,
    val grao: Bitmap
)

/**
 * Desenha as peças que não mudam e guarda o resultado.
 *
 * Isso é feito uma vez por tamanho de tela, fora da linha principal, porque são
 * centenas de milhares de pixels calculados um por um. Depois de pronto, cada quadro
 * só copia as imagens e redesenha a areia.
 */
internal object Renderizador {

    private var chaveGuardada: String? = null
    private var pecasGuardadas: Pecas? = null

    /** Já existem peças prontas para este tamanho? */
    fun prontas(plano: Plano): Pecas? =
        if (chaveGuardada == plano.chave) pecasGuardadas else null

    /**
     * Desenha tudo. Roda fora da linha principal e pode levar alguns segundos na
     * primeira vez; depois disso o resultado fica guardado.
     */
    fun desenhar(plano: Plano): Pecas {
        prontas(plano)?.let { return it }

        // Os grupos são desenhados ao mesmo tempo, um por núcleo disponível.
        val grupos: List<() -> List<Parte>> = listOf(
            { listOf(Pincel().vidroFrente(plano)) },
            {
                val pincel = Pincel()
                listOf(pincel.vidroTras(plano), pincel.anel(plano, true), pincel.anel(plano, false)) +
                    pincel.colunas(plano)
            },
            { listOf(Pincel().disco(plano, true)) },
            {
                val grao = AreiaCamada.grao(plano)
                listOf(
                    Pincel().disco(plano, false),
                    grao,
                    AreiaCamada.bulbo(plano, grao, true),
                    AreiaCamada.bulbo(plano, grao, false)
                )
            }
        )

        val linhas = minOf(grupos.size, maxOf(1, Runtime.getRuntime().availableProcessors()))
        val partes = HashMap<String, Parte>()
        if (linhas <= 1) {
            grupos.forEach { grupo -> grupo().forEach { partes[it.nome] = it } }
        } else {
            val pool = Executors.newFixedThreadPool(linhas)
            try {
                val tarefas = grupos.map { grupo -> pool.submit<List<Parte>> { grupo() } }
                tarefas.forEach { tarefa ->
                    runCatching { tarefa.get(120, TimeUnit.SECONDS) }
                        .onSuccess { lista -> lista.forEach { partes[it.nome] = it } }
                        .onFailure { erro -> Log.w("Agora", "Uma peça da ampulheta não ficou pronta", erro) }
                }
            } finally {
                pool.shutdownNow()
            }
        }
        // Se alguma peça falhou, ela é desenhada aqui mesmo, sem paralelismo.
        if (partes.size < 10) {
            grupos.forEach { grupo ->
                runCatching { grupo() }
                    .onSuccess { lista -> lista.forEach { parte -> partes.putIfAbsent(parte.nome, parte) } }
                    .onFailure { erro -> Log.w("Agora", "Refazer a peça da ampulheta também falhou", erro) }
            }
        }

        val tras = compor(plano, partes, listOf("discoBase", "colunaEsq", "colunaDir", "vidroTras"))
        val frente = compor(plano, partes, listOf("vidroFrente", "anelBase", "anelTopo", "discoTopo"))
        val topo = partes.getValue("areiaTopo")
        val base = partes.getValue("areiaBase")
        val grao = partes.getValue("grao")

        val pecas = Pecas(
            tras = tras,
            frente = frente,
            areiaTopo = bitmapDe(topo),
            areiaTopoX = topo.x,
            areiaTopoY = topo.y,
            areiaBase = bitmapDe(base),
            areiaBaseX = base.x,
            areiaBaseY = base.y,
            grao = bitmapDe(grao)
        )
        chaveGuardada = plano.chave
        pecasGuardadas = pecas
        return pecas
    }

    private fun bitmapDe(parte: Parte): Bitmap =
        Bitmap.createBitmap(parte.pixels, parte.w, parte.h, Bitmap.Config.ARGB_8888)

    private fun compor(plano: Plano, partes: Map<String, Parte>, nomes: List<String>): Bitmap {
        val destino = Bitmap.createBitmap(plano.largura, plano.altura, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(destino)
        nomes.forEach { nome ->
            partes[nome]?.let { parte ->
                val bmp = bitmapDe(parte)
                canvas.drawBitmap(bmp, parte.x.toFloat(), parte.y.toFloat(), null)
                bmp.recycle()
            }
        }
        return destino
    }
}
