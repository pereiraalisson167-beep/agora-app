package br.com.agora.ui.hourglass

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A ampulheta que mede o tempo da tarefa: a areia desce conforme o tempo passa e o
 * monte cresce embaixo. Não há número nenhum na tela — a areia é o relógio.
 *
 * As peças fixas (madeira, vidro, latão e a areia iluminada) são desenhadas uma vez
 * pelo [Renderizador]. A cada quadro só mudam o nível da areia, o fio que cai e os
 * grãos soltos.
 *
 * Portado do projeto original da ampulheta, com a areia guiada pelo tempo decorrido
 * da tarefa em vez de uma contagem numérica.
 */
class Ampulheta(context: Context) : View(context) {

    /** Avisa quando as peças ficaram prontas ou quando não deu para desenhá-las. */
    var aoFicarPronta: ((pronta: Boolean) -> Unit)? = null

    private var inicioDoCiclo = 0L
    private var totalSegundos = 0
    private var progresso = 0.0
    private var correndo = false

    private var plano: Plano? = null
    private var pecas: Pecas? = null
    private var desenhando = false
    private var falhou = false

    private var areiaBmp: Bitmap? = null
    private var areiaCanvas: Canvas? = null
    private var fxBmp: Bitmap? = null
    private var fxCanvas: Canvas? = null
    private var retrato: Bitmap? = null

    private var ultimoEstado: EstadoAreia? = null
    private var areiaSuja = true

    private val movimentoReduzido: Boolean = run {
        val escala = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        escala == 0f
    }

    // ---------------------------------------------------------------- pincéis
    private val liso = Paint(Paint.ANTI_ALIAS_FLAG)
    private val multiplica = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }
    private val clareia = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    private val soma = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val traco = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val copia = Paint(Paint.FILTER_BITMAP_FLAG)

    private val caminhoTopo = Path()
    private val caminhoMonte = Path()
    private val oval = RectF()

    private val granizo: Bitmap = criarFaisca()

    // ---------------------------------------------------------------- conversão de medidas
    private fun cx(x: Double): Float = ((plano?.ox ?: 0.0) + x * (plano?.s ?: 1.0)).toFloat()
    private fun cy(y: Double): Float = ((plano?.oy ?: 0.0) + y * (plano?.s ?: 1.0)).toFloat()
    private val escala: Double get() = plano?.s ?: 1.0

    // ---------------------------------------------------------------- estado de fora
    /**
     * Começa um ciclo: a areia leva [totalSeg] segundos para passar inteira de um bulbo
     * para o outro, contados a partir de [inicioUptime] (o relógio que não anda para trás).
     *
     * Quando [inicioUptime] está no futuro, a areia espera parada no bulbo de cima —
     * é assim que ela aguarda o fim da virada do "Mais 5 min".
     */
    fun definirCiclo(inicioUptime: Long, totalSeg: Int) {
        inicioDoCiclo = inicioUptime
        totalSegundos = totalSeg
        atualizarProgresso()
        invalidate()
    }

    /** A areia segue o tempo da tarefa: 0 no começo, 1 quando o tempo acaba. */
    private fun atualizarProgresso() {
        if (totalSegundos <= 0) {
            progresso = 0.0
            correndo = false
            return
        }
        val decorrido = agora() - inicioDoCiclo
        progresso = min(1.0, max(0.0, decorrido.toDouble() / (totalSegundos * 1000.0)))
        correndo = decorrido >= 0 && progresso < 1.0
    }

    /**
     * Vira a ampulheta de cabeça para baixo e devolve quantos milissegundos a virada leva.
     * Usada no "Mais 5 min": a areia começa um ciclo novo quando a virada termina.
     */
    fun virar(): Long {
        limparEfeitos()
        val p = pecas
        if (p == null || movimentoReduzido || plano == null) {
            animacao = Animacao(tipo = Tipo.VOLTAR, inicio = agora(), duracao = if (movimentoReduzido) 220L else 500L, de = progresso)
            invalidate()
            return animacao!!.duracao
        }
        guardarRetrato()
        areiaSuja = true
        animacao = Animacao(tipo = Tipo.VIRAR, inicio = agora(), duracao = 1150L, de = progresso)
        invalidate()
        return 1150L
    }

    // ---------------------------------------------------------------- ciclo de vida
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w < 40 || h < 40) return
        val s = min(w / G.BOX_W, h / G.BOX_H)
        val novo = Plano(
            s = s,
            ox = w / 2.0,
            oy = (h - G.BOX_H * s) / 2 - G.BOX_TOP * s,
            largura = w,
            altura = h
        )
        if (plano?.chave == novo.chave && pecas != null) return
        plano = novo
        soltarBitmaps()
        areiaBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        areiaCanvas = Canvas(areiaBmp!!)
        fxBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        fxCanvas = Canvas(fxBmp!!)
        areiaSuja = true
        ultimoEstado = null
        prepararPecas(novo)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        limparEfeitos()
        soltarBitmaps()
        pecas = null
    }

    private fun soltarBitmaps() {
        areiaCanvas = null
        fxCanvas = null
        areiaBmp?.recycle(); areiaBmp = null
        fxBmp?.recycle(); fxBmp = null
        retrato?.recycle(); retrato = null
    }

    private fun prepararPecas(plano: Plano) {
        Renderizador.prontas(plano)?.let {
            pecas = it
            falhou = false
            aoFicarPronta?.invoke(true)
            invalidate()
            return
        }
        if (desenhando) return
        desenhando = true
        falhou = false
        aoFicarPronta?.invoke(false)
        Thread({
            val resultado = runCatching { Renderizador.desenhar(plano) }
            post {
                desenhando = false
                resultado
                    .onSuccess { prontas ->
                        if (this.plano?.chave == plano.chave) {
                            pecas = prontas
                            falhou = false
                            areiaSuja = true
                            aoFicarPronta?.invoke(true)
                            invalidate()
                        }
                    }
                    .onFailure { erro ->
                        Log.w("Agora", "A ampulheta não pôde ser desenhada neste aparelho", erro)
                        falhou = true
                        aoFicarPronta?.invoke(false)
                    }
            }
        }, "ampulheta").start()
    }

    // ---------------------------------------------------------------- animação da virada
    private enum class Tipo { VIRAR, VOLTAR }

    private class Animacao(val tipo: Tipo, val inicio: Long, val duracao: Long, val de: Double)

    private var animacao: Animacao? = null

    private fun agora(): Long = SystemClock.uptimeMillis()

    private fun suavizar(t: Double): Double =
        if (t < 0.5) 4 * t * t * t else 1 - (-2 * t + 2).pow(3.0) / 2

    private fun guardarRetrato() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val bmp = retrato ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { retrato = it }
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        pecas?.let { p ->
            canvas.drawBitmap(p.tras, 0f, 0f, copia)
            areiaBmp?.let { canvas.drawBitmap(it, 0f, 0f, copia) }
            canvas.drawBitmap(p.frente, 0f, 0f, copia)
        }
    }

    // ---------------------------------------------------------------- desenho de cada quadro
    override fun onDraw(canvas: Canvas) {
        val plano = this.plano ?: return
        val instante = agora()
        atualizarProgresso()

        if (correndo && !fluindo) comecarFio(instante)
        else if (!correndo && fluindo) pararFio(instante)

        var p = progresso
        var virando = false
        var alfaRetrato = 0f
        var alfaPilha = 1f
        var alfaSombra = 1f
        var rotacao = 0f
        var levantar = 0f
        var encolher = 1f

        animacao?.let { anim ->
            val t = min(1.0, max(0.0, (instante - anim.inicio).toDouble() / anim.duracao))
            if (anim.tipo == Tipo.VOLTAR) {
                p = anim.de * (1 - suavizar(t))
            } else {
                virando = true
                val a = suavizar(t)
                val subida = sin(PI * t)
                rotacao = (a * 180).toFloat()
                levantar = (-subida * 10 * escala).toFloat()
                encolher = (1 - 0.14 * subida).toFloat()
                val cruzar = min(1.0, max(0.0, (t - 0.72) / 0.28))
                alfaRetrato = (1 - cruzar).toFloat()
                alfaPilha = cruzar.toFloat()
                alfaSombra = (1 - 0.55 * subida).toFloat()
                p = 0.0
            }
            if (t >= 1.0) {
                animacao = null
                if (anim.tipo == Tipo.VIRAR) {
                    alfaRetrato = 0f
                    alfaPilha = 1f
                    alfaSombra = 1f
                }
            }
        }

        desenharSombra(canvas, alfaSombra)

        val pecas = this.pecas
        if (pecas == null) {
            if (animacao != null || desenhando) invalidate()
            return
        }

        val estado = G.estadoDaAreia(p)
        if (areiaSuja || mudouAreia(estado)) {
            redesenharAreia(estado)
            areiaSuja = false
        }
        ultimoEstado = estado

        if (alfaPilha > 0.004f) {
            copia.alpha = (alfaPilha * 255).toInt()
            canvas.drawBitmap(pecas.tras, 0f, 0f, copia)
            areiaBmp?.let { canvas.drawBitmap(it, 0f, 0f, copia) }
            if (!virando) {
                atualizarParticulas(estado, instante)
                desenharEfeitos(instante, estado)
                fxBmp?.let { canvas.drawBitmap(it, 0f, 0f, copia) }
            }
            canvas.drawBitmap(pecas.frente, 0f, 0f, copia)
            copia.alpha = 255
        }

        if (alfaRetrato > 0.004f) {
            retrato?.let { bmp ->
                canvas.save()
                canvas.translate(0f, levantar)
                canvas.rotate(rotacao, cx(0.0), cy(G.NECK_Y))
                canvas.scale(encolher, encolher, cx(0.0), cy(G.NECK_Y))
                copia.alpha = (alfaRetrato * 255).toInt()
                canvas.drawBitmap(bmp, 0f, 0f, copia)
                copia.alpha = 255
                canvas.restore()
            }
        }

        if (animacao != null || correndo || efeitosVivos()) invalidate()
    }

    private fun mudouAreia(estado: EstadoAreia): Boolean {
        val a = ultimoEstado ?: return true
        val s = escala
        return a.fase != estado.fase ||
            abs(a.ds - estado.ds) * s > 0.18 ||
            abs(a.hApex - estado.hApex) * s > 0.18 ||
            (a.p < 0.07 && abs(a.p - estado.p) > 0.002)
    }

    // ---------------------------------------------------------------- sombra no chão
    private fun desenharSombra(canvas: Canvas, alfa: Float) {
        if (alfa <= 0.004f) return
        val k = G.k(G.BASE_BOT_Y)
        val s = escala.toFloat()

        canvas.save()
        canvas.translate(cx(0.0), cy(G.BASE_BOT_Y + 2))
        canvas.scale(s, (s * k * 1.7).toFloat())
        liso.reset()
        liso.isAntiAlias = true
        liso.shader = RadialGradient(
            0f, 0f, 640f,
            intArrayOf(Color.argb((0.78f * alfa * 255).toInt(), 0, 0, 0), Color.argb((0.5f * alfa * 255).toInt(), 0, 0, 0), Color.TRANSPARENT),
            floatArrayOf(0f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, 640f, liso)
        canvas.restore()

        canvas.save()
        canvas.translate(cx(0.0), cy(G.BASE_BOT_Y + G.R * k + 30))
        canvas.scale(s, s * 0.12f)
        liso.shader = RadialGradient(
            0f, 0f, 520f,
            intArrayOf(Color.argb((0.16f * alfa * 255).toInt(), 255, 170, 70), Color.argb(0, 255, 170, 70)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(0f, 0f, 520f, liso)
        canvas.restore()
        liso.shader = null
    }

    // ---------------------------------------------------------------- a areia
    private val luzTmp = DoubleArray(3)
    private val refCone: Double = run {
        val o = DoubleArray(3)
        G.luz(0.0, cos(atan(G.TAN_T)), sin(atan(G.TAN_T)), o)
        1 / (0.5 * o[0] + 0.4 * o[1] + 0.1 * o[2])
    }

    /** A areia escurece para laranja: o azul cai primeiro, depois o verde. */
    private fun corAreia(fIn: Double): Int {
        val f = max(0.0, fIn)
        val r = (255 * min(1.0, f.pow(0.75))).toInt()
        val g = (255 * min(1.0, f.pow(1.25))).toInt()
        val b = (255 * min(1.0, f.pow(2.4))).toInt()
        return Color.rgb(r, g, b)
    }

    private fun corDaFace(nx: Double, ny: Double, nz: Double, mul: Double): Int {
        G.luz(nx, ny, nz, luzTmp)
        val l = 0.5 * luzTmp[0] + 0.4 * luzTmp[1] + 0.1 * luzTmp[2]
        return corAreia(l * refCone * mul)
    }

    private fun caminhoCorpoTopo(st: EstadoAreia) {
        val c = caminhoTopo
        c.reset()
        val n = 56
        for (i in 0..n) {
            val d = st.ds * (1 - i.toDouble() / n).pow(1.5)
            val x = cx(-G.rDraw(d))
            val y = cy(G.NECK_Y - d)
            if (i == 0) c.moveTo(x, y) else c.lineTo(x, y)
        }
        for (i in 0..n) {
            val d = st.ds * (i.toDouble() / n).pow(1.5)
            c.lineTo(cx(G.rDraw(d)), cy(G.NECK_Y - d))
        }
        val rx = (st.rs * escala).toFloat()
        val ry = max(0.5, st.rs * st.ks * escala).toFloat()
        oval.set(cx(0.0) - rx, cy(st.ys) - ry, cx(0.0) + rx, cy(st.ys) + ry)
        c.arcTo(oval, 0f, -180f, false)
        c.close()
    }

    private fun caminhoDoMonte(st: EstadoAreia) {
        val c = caminhoMonte
        c.reset()
        val kB = G.k(G.BASE_TOP_Y)
        if (st.fase == 1) {
            c.moveTo(cx(0.0), cy(st.yApex))
            c.lineTo(cx(st.a), cy(G.BASE_TOP_Y))
            val rx = (st.a * escala).toFloat()
            val ry = (st.a * kB * escala).toFloat()
            oval.set(cx(0.0) - rx, cy(G.BASE_TOP_Y) - ry, cx(0.0) + rx, cy(G.BASE_TOP_Y) + ry)
            c.arcTo(oval, 0f, 180f, false)
            c.close()
            return
        }
        val n = 28
        val rf = G.rDraw(G.L)
        c.moveTo(cx(0.0), cy(st.yApex))
        c.lineTo(cx(st.rf), cy(st.yf))
        for (i in 0..n) {
            val h = st.hf * (1 - i.toDouble() / n)
            c.lineTo(cx(G.rDraw(G.L - h)), cy(G.BASE_TOP_Y - h))
        }
        val rx = (rf * escala).toFloat()
        val ry = (rf * kB * escala).toFloat()
        oval.set(cx(0.0) - rx, cy(G.BASE_TOP_Y) - ry, cx(0.0) + rx, cy(G.BASE_TOP_Y) + ry)
        c.arcTo(oval, 0f, 180f, false)
        for (i in 0..n) {
            val h = st.hf * (i.toDouble() / n)
            c.lineTo(cx(-G.rDraw(G.L - h)), cy(G.BASE_TOP_Y - h))
        }
        c.close()
    }

    private fun redesenharAreia(st: EstadoAreia) {
        val canvas = areiaCanvas ?: return
        val pecas = this.pecas ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        if (st.ds > 0.4) {
            canvas.save()
            caminhoCorpoTopo(st)
            canvas.clipPath(caminhoTopo)
            canvas.drawBitmap(pecas.areiaTopo, pecas.areiaTopoX.toFloat(), pecas.areiaTopoY.toFloat(), copia)
            canvas.restore()
            desenharSuperficie(canvas, st, pecas)
        }
        if (st.fase > 0) {
            canvas.save()
            caminhoDoMonte(st)
            canvas.clipPath(caminhoMonte)
            canvas.drawBitmap(pecas.areiaBase, pecas.areiaBaseX.toFloat(), pecas.areiaBaseY.toFloat(), copia)
            canvas.restore()
            desenharCone(canvas, st, pecas)
        }
    }

    /** A superfície da areia de cima, com a cratera que o fio abre no meio. */
    private fun desenharSuperficie(canvas: Canvas, st: EstadoAreia, pecas: Pecas) {
        val cxv = cx(0.0)
        val cyv = cy(st.ys)
        val rx = (st.rs * escala).toFloat()
        val ry = max(0.8, st.rs * st.ks * escala).toFloat()
        if (rx < 0.6f) return
        canvas.save()
        oval.set(cxv - rx, cyv - ry, cxv + rx, cyv + ry)
        val recorte = Path()
        recorte.addOval(oval, Path.Direction.CW)
        canvas.clipPath(recorte)
        val area = RectF(cxv - rx - 1, cyv - ry - 1, cxv + rx + 1, cyv + ry + 1)

        liso.reset(); liso.isAntiAlias = true
        liso.shader = BitmapShader(pecas.grao, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        canvas.drawRect(area, liso)
        liso.shader = null

        multiplica.shader = LinearGradient(
            cxv - rx, 0f, cxv + rx, 0f,
            intArrayOf(corAreia(0.97), corAreia(1.0), corAreia(0.9)),
            floatArrayOf(0f, 0.4f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(area, multiplica)
        multiplica.shader = LinearGradient(
            0f, cyv - ry, 0f, cyv + ry,
            intArrayOf(corAreia(0.86), corAreia(0.99), corAreia(1.0)),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(area, multiplica)
        multiplica.shader = null

        val cratera = Cor.clamp((st.p - 0.004) / 0.06, 0.0, 1.0) *
            (0.5 + 0.5 * Cor.clamp((st.p - 0.5) / 0.5, 0.0, 1.0))
        if (cratera > 0) {
            canvas.save()
            canvas.translate(cxv, cyv - ry * 0.06f)
            canvas.scale(1f, ry / rx)
            liso.shader = RadialGradient(
                0f, 0f, rx * 0.62f,
                intArrayOf(
                    Color.argb((0.6 * cratera * 255).toInt(), 150, 72, 8),
                    Color.argb((0.36 * cratera * 255).toInt(), 206, 132, 40),
                    Color.argb(0, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.35f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawRect(-rx, -rx, rx, rx, liso)
            liso.shader = null
            canvas.restore()
        }

        canvas.save()
        canvas.translate(cxv, cyv)
        canvas.scale(1f, ry / rx)
        liso.shader = RadialGradient(
            0f, 0f, rx,
            intArrayOf(Color.argb(0, 255, 255, 255), Color.argb((0.6f * 255).toInt(), 196, 120, 40)),
            floatArrayOf(0.78f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(-rx, -rx, rx, rx, liso)
        liso.shader = null
        canvas.restore()
        canvas.restore()

        // fio de luz na beirada da superfície
        traco.xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        traco.color = Color.argb((0.30f * 255).toInt(), 255, 214, 120)
        traco.strokeWidth = max(1.0, 1.1).toFloat()
        val arco = Path()
        oval.set(cxv - rx * 0.985f, cyv - ry * 0.94f, cxv + rx * 0.985f, cyv + ry * 0.94f)
        arco.arcTo(oval, 18f, 144f, true)
        canvas.drawPath(arco, traco)
        traco.xfermode = null
    }

    /** O cone de areia que cresce embaixo, com a luz girando em volta dele. */
    private fun desenharCone(canvas: Canvas, st: EstadoAreia, pecas: Pecas) {
        val baseR: Double
        val baseY: Double
        val k: Double
        if (st.fase == 1) {
            baseR = st.a; baseY = G.BASE_TOP_Y; k = G.k(G.BASE_TOP_Y)
        } else {
            baseR = st.rf; baseY = st.yf; k = st.kf
        }
        if (baseR * escala < 0.6) return
        val ax = cx(0.0)
        val ay = cy(st.yApex)
        val esq = cx(-baseR) - 2
        val larg = (2 * baseR * escala + 4).toFloat()
        val alt = (cy(baseY) + baseR * k * escala - ay + 4).toFloat()
        val area = RectF(esq, ay - 2, esq + larg, ay - 2 + alt)

        canvas.save()
        val recorte = Path()
        recorte.moveTo(ax, ay)
        recorte.lineTo(cx(baseR), cy(baseY))
        val rx = (baseR * escala).toFloat()
        val ry = max(0.5, baseR * k * escala).toFloat()
        oval.set(ax - rx, cy(baseY) - ry, ax + rx, cy(baseY) + ry)
        recorte.arcTo(oval, 0f, 180f, false)
        recorte.close()
        canvas.clipPath(recorte)

        liso.reset(); liso.isAntiAlias = true
        liso.shader = BitmapShader(pecas.grao, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        canvas.drawRect(area, liso)
        liso.shader = null

        multiplica.shader = sombraDoCone(ax, ay)
        canvas.drawRect(area, multiplica)
        multiplica.shader = LinearGradient(
            0f, ay, 0f, ay + alt,
            intArrayOf(corAreia(1.0), corAreia(0.95), corAreia(0.68)),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(area, multiplica)
        multiplica.shader = null

        clareia.shader = RadialGradient(
            (ax - baseR * 0.18 * escala).toFloat(), ay + alt * 0.28f,
            (baseR * 0.75 * escala).toFloat(),
            intArrayOf(Color.argb((0.34f * 255).toInt(), 255, 214, 110), Color.argb(0, 255, 214, 110)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(area, clareia)
        clareia.shader = null

        // sulcos de desabamento descendo do topo do cone
        traco.xfermode = null
        traco.strokeWidth = 0.9f
        for (i in 0 until 11) {
            val sp = -0.92 + 1.84 * ((i * 0.618) % 1.0)
            val cp = raiz(1 - sp * sp)
            val ex = cx(baseR * sp)
            val ey = (cy(baseY) + baseR * k * cp * escala).toFloat()
            traco.color = if (i % 2 == 1) Color.argb((0.10f * 255).toInt(), 120, 74, 22)
            else Color.argb((0.10f * 255).toInt(), 255, 236, 170)
            canvas.drawLine(ax, ay, ex, ey, traco)
        }
        canvas.restore()
    }

    /**
     * A luz em volta do cone. No original é um degradê cônico; aqui ele é montado com
     * um degradê em leque, com as posições giradas para casar com o mesmo ângulo.
     */
    private fun sombraDoCone(ax: Float, ay: Float): Shader {
        val a = PI / 2 - atan(G.TAN_T)
        val inicio = atan(G.TAN_T) / (2 * PI)      // onde o leque começa, contado do norte
        val giro = (inicio + 0.75) % 1.0           // o leque do Android começa no leste
        val vao = 2 * a / (2 * PI)
        val n = 128
        val cores = IntArray(n)
        val posicoes = FloatArray(n)
        for (i in 0 until n) {
            val pos = i.toDouble() / (n - 1)
            posicoes[i] = pos.toFloat()
            var local = (pos - giro) % 1.0
            if (local < 0) local += 1.0
            val t = Cor.clamp(local / vao, 0.0, 1.0)
            val ang = a - 2 * a * t
            val sp = Cor.clamp(Math.tan(ang) * G.TAN_T, -1.0, 1.0)
            val cp = raiz(1 - sp * sp)
            cores[i] = corDaFace(G.SIN_T * sp, G.COS_T, G.SIN_T * cp, 1.0)
        }
        return SweepGradient(ax, ay, cores, posicoes)
    }

    // ---------------------------------------------------------------- fio de areia e grãos soltos
    private class Grao(
        var x: Double, var y: Double, var vx: Double, var vy: Double,
        var t: Double, var k: Int, var b: Double, var z: Double, var vida: Double
    )

    private class Poeira(
        var x: Double, var y: Double, var vx: Double, var vy: Double,
        var t: Double, var vida: Double, var s: Double
    )

    private class Trecho(val inicio: Long, var fim: Long?)

    private companion object {
        const val GRAV = 5200.0
        const val V0 = 140.0
        val PESCOCO = G.NECK_Y + 3
        fun queda(t: Double): Double = if (t <= 0) 0.0 else V0 * t + 0.5 * GRAV * t * t
    }

    private val trechos = ArrayList<Trecho>(8)
    private val graos = ArrayList<Grao>(256)
    private val poeiras = ArrayList<Poeira>(64)
    private var acumulado = 0.0
    private var fluindo = false
    private var aparicao = 0.0
    private var ultimoQuadro = 0L

    private class Brilho(val a: Double, val f: Double, val w: Double, val ph: Double, val s: Double, val u: Double)

    private val brilhosTopo: List<Brilho>
    private val brilhosMonte: List<Brilho>

    init {
        val r = Mulberry32(31)
        brilhosTopo = (0 until 26).map {
            Brilho(r.next() * PI * 2, sqrt(r.next()) * 0.94, 0.8 + r.next() * 1.6, r.next() * 6.28, 0.7 + r.next() * 0.8, 0.0)
        }
        brilhosMonte = (0 until 30).map {
            Brilho(0.0, 0.08 + r.next() * 0.9, 0.8 + r.next() * 1.6, r.next() * 6.28, 0.7 + r.next() * 0.8, (r.next() * 2 - 1) * 0.95)
        }
    }

    private fun criarFaisca(): Bitmap {
        val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
        pincel.shader = RadialGradient(
            16f, 16f, 16f,
            intArrayOf(
                Color.argb(255, 255, 250, 225),
                Color.argb((0.85f * 255).toInt(), 255, 226, 150),
                Color.argb((0.18f * 255).toInt(), 255, 190, 80),
                Color.argb(0, 255, 170, 60)
            ),
            floatArrayOf(0f, 0.18f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 32f, 32f, pincel)
        pincel.shader = null
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 1.2f
        pincel.color = Color.argb((0.75f * 255).toInt(), 255, 240, 200)
        canvas.drawLine(16f, 3f, 16f, 29f, pincel)
        canvas.drawLine(3f, 16f, 29f, 16f, pincel)
        return bmp
    }

    private fun comecarFio(instante: Long) {
        trechos.add(Trecho(instante, null))
        fluindo = true
    }

    private fun pararFio(instante: Long) {
        trechos.lastOrNull()?.let { if (it.fim == null) it.fim = instante }
        fluindo = false
    }

    private fun limparEfeitos() {
        trechos.clear()
        graos.clear()
        poeiras.clear()
        fluindo = false
        aparicao = 0.0
        fxCanvas?.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    }

    private fun efeitosVivos(): Boolean =
        trechos.isNotEmpty() || graos.isNotEmpty() || poeiras.isNotEmpty() || aparicao > 0.0

    private fun gauss(): Double = (Random.nextDouble() + Random.nextDouble() + Random.nextDouble() - 1.5) * 1.15

    private fun atualizarParticulas(st: EstadoAreia, instante: Long) {
        val dt = if (ultimoQuadro == 0L) 0.016 else min(0.05, max(0.0, (instante - ultimoQuadro) / 1000.0))
        ultimoQuadro = instante

        val taxa = if (movimentoReduzido) 140.0 else 520.0
        if (fluindo && st.ds > 0.5) {
            acumulado += dt * taxa * min(1.0, 0.35 + st.ds / 60)
            while (acumulado >= 1) {
                acumulado -= 1
                graos.add(
                    Grao(
                        x = (Random.nextDouble() - 0.5) * 7,
                        y = PESCOCO + Random.nextDouble() * 5,
                        vx = gauss() * 17,
                        vy = V0 * (0.7 + Random.nextDouble() * 0.6),
                        t = 0.0, k = 0,
                        b = 0.5 + Random.nextDouble() * 0.5,
                        z = 2.4 + Random.nextDouble() * 2.6,
                        vida = 0.0
                    )
                )
            }
        }

        var i = graos.size - 1
        while (i >= 0) {
            val p = graos[i]
            p.t += dt
            var removido = false
            if (p.k == 0) {
                p.vy += GRAV * dt; p.x += p.vx * dt; p.y += p.vy * dt
                p.vx += gauss() * 30 * dt * min(1.0, (p.y - PESCOCO) / 200)
                val sy = G.alturaDoMonte(p.x, st)
                if (p.y >= sy) {
                    if (Random.nextDouble() < 0.38) {
                        p.k = 1; p.y = sy - 1
                        p.vy = -(50 + Random.nextDouble() * 170)
                        p.vx = (if (Random.nextDouble() < 0.5) -1 else 1) * (40 + Random.nextDouble() * 170) + p.vx
                    } else {
                        graos.removeAt(i); removido = true
                    }
                }
            } else if (p.k == 1) {
                p.vy += GRAV * 0.85 * dt; p.x += p.vx * dt; p.y += p.vy * dt
                val sy = G.alturaDoMonte(p.x, st)
                if (p.vy > 0 && p.y >= sy) {
                    p.k = 2; p.t = 0.0
                    p.vida = 0.25 + Random.nextDouble() * 0.5
                    p.vx = (if (p.x < 0) -1 else 1) * (50 + Random.nextDouble() * 90)
                    p.y = sy - 1.2
                }
            } else {
                p.x += p.vx * dt
                p.vx *= 1 - 2.4 * dt
                p.y = G.alturaDoMonte(p.x, st) - 1.2
                if (p.t > p.vida) { graos.removeAt(i); removido = true }
                i--
                continue
            }
            if (!removido && (p.y > G.BASE_TOP_Y + 20 || abs(p.x) > 400)) graos.removeAt(i)
            i--
        }

        // poeira brilhando em volta do fio
        aparicao = Cor.clamp(
            aparicao + (if (fluindo && st.ds > 0.5) dt * 1.5 else -dt * 1.2),
            0.0, 1.0
        )
        val querem = if (aparicao > 0) (if (movimentoReduzido) 10 else 40) else 0
        while (poeiras.size < querem) poeiras.add(novaPoeira(st))
        var j = poeiras.size - 1
        while (j >= 0) {
            val m = poeiras[j]
            m.t += dt; m.x += m.vx * dt; m.y += m.vy * dt
            if (m.t > m.vida) {
                if (poeiras.size > querem) poeiras.removeAt(j) else poeiras[j] = novaPoeira(st)
            }
            j--
        }
    }

    private fun novaPoeira(st: EstadoAreia): Poeira {
        if (Random.nextDouble() < 0.28 && st.ds > 30) {
            return Poeira(
                x = (Random.nextDouble() * 2 - 1) * st.rs * 0.6,
                y = st.ys - 10 - Random.nextDouble() * 90,
                vx = (Random.nextDouble() - 0.5) * 10,
                vy = -4 - Random.nextDouble() * 8,
                t = 0.0, vida = 1 + Random.nextDouble() * 1.6, s = 0.5 + Random.nextDouble() * 0.7
            )
        }
        val vao = max(20.0, st.yApex - PESCOCO - 20)
        if (Random.nextDouble() < 0.35) {
            return Poeira(
                x = gauss() * 70,
                y = st.yApex - 4 - Random.nextDouble() * 60,
                vx = (Random.nextDouble() - 0.5) * 40,
                vy = -10 - Random.nextDouble() * 25,
                t = 0.0, vida = 0.5 + Random.nextDouble() * 0.9, s = 0.45 + Random.nextDouble() * 0.6
            )
        }
        return Poeira(
            x = gauss() * 34,
            y = PESCOCO + 14 + Random.nextDouble() * vao,
            vx = (Random.nextDouble() - 0.5) * 14,
            vy = (Random.nextDouble() - 0.3) * 18,
            t = 0.0, vida = 0.9 + Random.nextDouble() * 1.6, s = 0.5 + Random.nextDouble() * 0.8
        )
    }

    private fun desenharEfeitos(instante: Long, st: EstadoAreia) {
        val canvas = fxCanvas ?: return
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.save()
        canvas.translate(cx(0.0), cy(0.0))
        canvas.scale(escala.toFloat(), escala.toFloat())

        val apex = st.yApex
        var i = trechos.size - 1
        while (i >= 0) {
            val tr = trechos[i]
            val cabeca = PESCOCO + queda((instante - tr.inicio) / 1000.0)
            val cauda = tr.fim?.let { PESCOCO + queda((instante - it) / 1000.0) } ?: PESCOCO
            if (cauda >= apex - 0.5) { trechos.removeAt(i); i--; continue }
            val ya = max(PESCOCO, cauda)
            val yb = min(cabeca, apex)
            if (yb <= ya) { i--; continue }
            val fino = if (tr.fim == null) Cor.clamp(st.ds / 40, 0.35, 1.0) else 0.8

            fun fio(cor: Int, largura: Double) {
                soma.style = Paint.Style.STROKE
                soma.strokeCap = Paint.Cap.ROUND
                soma.shader = null
                soma.color = cor
                soma.strokeWidth = (largura * fino).toFloat()
                canvas.drawLine(0f, ya.toFloat(), 0f, yb.toFloat(), soma)
            }
            fio(Color.argb((0.07f * 255).toInt(), 255, 170, 50), 54.0)
            fio(Color.argb((0.17f * 255).toInt(), 255, 190, 70), 25.0)

            soma.style = Paint.Style.STROKE
            soma.strokeWidth = (11.5 * fino).toFloat()
            soma.shader = LinearGradient(
                0f, ya.toFloat(), 0f, yb.toFloat(),
                intArrayOf(
                    Color.argb((0.95f * 255).toInt(), 255, 226, 140),
                    Color.argb((0.9f * 255).toInt(), 255, 205, 92),
                    Color.argb((0.85f * 255).toInt(), 255, 186, 64)
                ),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawLine(0f, ya.toFloat(), 0f, yb.toFloat(), soma)
            soma.shader = null
            fio(Color.argb((0.55f * 255).toInt(), 255, 248, 214), 3.0)

            // tufos escorregando pelo fio
            soma.style = Paint.Style.FILL
            soma.color = Color.argb((0.20f * 255).toInt(), 255, 236, 170)
            val tf = sqrt(2 * max(1.0, yb - PESCOCO) / GRAV)
            for (n in 0 until 12) {
                val ph = ((instante / 1000.0) / tf + n / 12.0) % 1.0
                val yy = PESCOCO + queda(ph * tf)
                if (yy < ya || yy > yb) continue
                canvas.drawRect(
                    (-5 * fino).toFloat(), (yy - 7).toFloat(),
                    (5 * fino).toFloat(), (yy + 7).toFloat(), soma
                )
            }

            // faíscas ao longo do fio
            for (n in 0 until 7) {
                val yy = ya + Random.nextDouble() * (yb - ya)
                val z = 8 + Random.nextDouble() * 10
                faisca(canvas, (Random.nextDouble() - 0.5) * 12 - z / 2, yy - z / 2, z, 0.35 + Random.nextDouble() * 0.5)
            }

            // clarão onde o fio bate no monte
            if (cabeca >= apex - 1 && tr.fim == null) {
                faiscaEm(canvas, -36.0, apex - 30, 72.0, 52.0, 0.5 * fino)
            }
            i--
        }

        for (p in graos) {
            if (p.k == 0) {
                soma.style = Paint.Style.STROKE
                soma.strokeCap = Paint.Cap.ROUND
                soma.shader = null
                soma.color = Color.argb((0.7 * p.b * 255).toInt(), 255, 222, 132)
                soma.strokeWidth = p.z.toFloat()
                val comp = min(20.0, p.vy * 0.008)
                canvas.drawLine(p.x.toFloat(), (p.y - comp).toFloat(), p.x.toFloat(), p.y.toFloat(), soma)
            } else {
                val a = if (p.k == 2) 0.8 * (1 - p.t / p.vida) else 0.85
                soma.style = Paint.Style.FILL
                soma.shader = null
                soma.color = Color.argb((Cor.clamp(a * p.b, 0.0, 1.0) * 255).toInt(), 255, 220, 120)
                val z = p.z * 1.15
                canvas.drawRect(
                    (p.x - z / 2).toFloat(), (p.y - z / 2).toFloat(),
                    (p.x + z / 2).toFloat(), (p.y + z / 2).toFloat(), soma
                )
            }
        }

        val tt = instante / 1000.0
        for (m in poeiras) {
            val a = sin(PI * Cor.clamp(m.t / m.vida, 0.0, 1.0)) * aparicao *
                (0.4 + 0.6 * max(0.0, sin(tt * 9 * m.s + m.vida * 7)))
            if (a <= 0.02) continue
            val z = 15 * m.s
            faisca(canvas, m.x - z / 2, m.y - z / 2, z, a)
        }

        // lampejos na superfície da areia
        if (st.ds > 2) {
            for (s in brilhosTopo) {
                val v = sin(tt * s.w + s.ph)
                if (v < 0.8) continue
                val a = ((v - 0.8) / 0.2).pow(2.0)
                val x = cos(s.a) * s.f * st.rs * 0.96
                val y = st.ys + sin(s.a) * s.f * st.rs * st.ks
                val z = 15 * s.s
                faisca(canvas, x - z / 2, y - z / 2, z, a)
            }
        }
        if (st.fase > 0) {
            val baseR = if (st.fase == 1) st.a else st.rf
            val k = if (st.fase == 1) G.k(G.BASE_TOP_Y) else st.kf
            for (s in brilhosMonte) {
                val v = sin(tt * s.w + s.ph)
                if (v < 0.86) continue
                val a = ((v - 0.86) / 0.14).pow(2.0)
                val rr = s.f * baseR
                val cp = raiz(1 - s.u * s.u)
                val x = rr * s.u
                val y = st.yApex + rr * G.TAN_T + rr * k * cp
                val z = 15 * s.s
                faisca(canvas, x - z / 2, y - z / 2, z, a * 0.95)
            }
        }
        canvas.restore()
    }

    private val destinoFaisca = RectF()

    private fun faisca(canvas: Canvas, x: Double, y: Double, z: Double, alfa: Double) {
        faiscaEm(canvas, x, y, z, z, alfa)
    }

    private fun faiscaEm(canvas: Canvas, x: Double, y: Double, w: Double, h: Double, alfa: Double) {
        soma.style = Paint.Style.FILL
        soma.shader = null
        soma.color = Color.WHITE
        soma.alpha = (Cor.clamp(alfa, 0.0, 1.0) * 255).toInt()
        destinoFaisca.set(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat())
        canvas.drawBitmap(granizo, null, destinoFaisca, soma)
        soma.alpha = 255
    }
}
