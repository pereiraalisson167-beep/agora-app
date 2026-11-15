package br.com.agora.ui.hourglass

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * As contas de cor, de ruído e do ambiente do estúdio, usadas pelos desenhos das peças.
 * Portado do projeto original da ampulheta.
 */
internal object Cor {
    fun clamp(x: Double, a: Double, b: Double): Double = if (x < a) a else if (x > b) b else x

    /** Degradê suave entre [a] e [b]. */
    fun sstep(a: Double, b: Double, x: Double): Double {
        val t = clamp((x - a) / (b - a), 0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    /** Aproxima o brilho alto ao que a tela consegue mostrar. */
    fun aces(cIn: Double): Double {
        val c = if (cIn < 0) 0.0 else cIn
        return clamp((c * (2.51 * c + 0.03)) / (c * (2.43 * c + 0.59) + 0.14), 0.0, 1.0)
    }

    fun enc(c: Double): Double = c.pow(1 / 2.2)

    private fun byte(v: Double): Int {
        val n = round(v).toInt()
        return if (n < 0) 0 else if (n > 255) 255 else n
    }

    /** Monta o pixel a partir de cor e transparência, sem multiplicar pelo alfa. */
    fun pack(r: Double, g: Double, b: Double, a: Double): Int =
        (byte(a * 255) shl 24) or (byte(r * 255) shl 16) or (byte(g * 255) shl 8) or byte(b * 255)

    /** Cor de material opaco: do brilho cru para o pixel da tela. */
    fun packLin(r: Double, g: Double, b: Double, a: Double): Int =
        pack(enc(aces(r)), enc(aces(g)), enc(aces(b)), a)
}

/** O mesmo sorteio do projeto original, para o desenho sair sempre igual. */
internal class Mulberry32(semente: Int) {
    private var a: Int = semente

    fun next(): Double {
        a += 0x6D2B79F5
        var t = (a xor (a ushr 15)) * (1 or a)
        t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
        return ((t xor (t ushr 14)).toLong() and 0xFFFFFFFFL).toDouble() / 4294967296.0
    }
}

/** Ruído de valor e sua soma em oitavas: a textura da madeira vem daqui. */
internal object Ruido {
    private val perm = IntArray(512)
    private val rv = DoubleArray(256)

    init {
        val r = Mulberry32(9137)
        val p = IntArray(256) { it }
        for (i in 255 downTo 1) {
            val j = (r.next() * (i + 1)).toInt()
            val t = p[i]; p[i] = p[j]; p[j] = t
        }
        for (i in 0 until 512) perm[i] = p[i and 255]
        for (i in 0 until 256) rv[i] = r.next()
    }

    fun vnoise(x: Double, y: Double): Double {
        val xi = floor(x).toInt()
        val yi = floor(y).toInt()
        val xf = x - xi
        val yf = y - yi
        val u = xf * xf * (3 - 2 * xf)
        val v = yf * yf * (3 - 2 * yf)
        val xx = xi and 255
        val yy = yi and 255
        val x1 = (xx + 1) and 255
        val y1 = (yy + 1) and 255
        val a = rv[perm[perm[xx] + yy]]
        val b = rv[perm[perm[x1] + yy]]
        val c = rv[perm[perm[xx] + y1]]
        val d = rv[perm[perm[x1] + y1]]
        return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v
    }

    fun fbm(x0: Double, y0: Double, oct: Int): Double {
        var s = 0.0
        var a = 0.5
        var n = 0.0
        var x = x0
        var y = y0
        for (i in 0 until oct) {
            s += a * vnoise(x, y)
            n += a
            x = x * 2.03 + 17.1
            y = y * 2.03 + 3.7
            a *= 0.5
        }
        return s / n
    }
}

/**
 * O estúdio em volta da ampulheta: duas janelas, o brilho do fundo e o reflexo do chão.
 * Direções: x para a direita, y para cima, z na direção da câmera.
 */
internal object Ambiente {
    private fun janela(
        az: Double, el: Double, a0: Double, a1: Double, e0: Double, e1: Double,
        cols: Int, rows: Int, soft: Boolean
    ): Double {
        val ew = if (soft) 0.09 else 0.02
        if (az < a0 - ew || az > a1 + ew || el < e0 - ew || el > e1 + ew) return 0.0
        val edge = Cor.sstep(a0 - ew, a0 + ew, az) * (1 - Cor.sstep(a1 - ew, a1 + ew, az)) *
            Cor.sstep(e0 - ew, e0 + ew, el) * (1 - Cor.sstep(e1 - ew, e1 + ew, el))
        if (edge <= 0) return 0.0
        val cw = (a1 - a0) / cols
        val rh = (e1 - e0) / rows
        val cu = (az - a0) / cw
        val cv = (el - e0) / rh
        val fu = abs(cu - round(cu)) * cw
        val fv = abs(cv - round(cv)) * rh
        val bar = 0.0075
        val m = if (soft) 1.0 else Cor.sstep(bar, bar + 0.006, fu) * Cor.sstep(bar, bar + 0.006, fv)
        val fy = (el - e0) / (e1 - e0)
        val fx = (az - a0) / (a1 - a0)
        val pane = 0.86 + 0.14 * sin(floor(cu) * 2.1 + floor(cv) * 1.3)
        return edge * m * pane * (0.10 + 0.90 * fy * fy) * (0.62 + 0.38 * (1 - fx))
    }

    /** A cor que vem do ambiente na direção ([x], [y], [z]). */
    fun env(x: Double, y: Double, z: Double, o: DoubleArray, soft: Boolean = false) {
        val el = asin(Cor.clamp(y, -1.0, 1.0))
        val az = atan2(x, z)
        var r = 0.020
        var g = 0.014
        var b = 0.010
        val back = Cor.clamp((0.25 - z) / 0.8, 0.0, 1.0)
        val hz = exp(-(el * el) / 0.25)
        val right = exp(-((az - 2.25) * (az - 2.25)) / 0.45)
        val left = exp(-((az + 2.3) * (az + 2.3)) / 0.5)
        val glow = back * hz * (0.62 * right + 0.42 * left + 0.10)
        r += glow * 0.70; g += glow * 0.42; b += glow * 0.18
        if (y < 0.03) {
            val f = Cor.sstep(0.03, -0.10, y)
            val near = exp(-((y + 0.08) * (y + 0.08)) / 0.012) * (0.4 + 0.6 * back)
            r += f * (0.06 + 0.30 * near - r * 0.5)
            g += f * (0.030 + 0.15 * near - g * 0.5)
            b += f * (0.014 + 0.06 * near - b * 0.5)
        }
        val w1 = janela(az, el, -2.25, -1.05, -0.12, 0.98, 2, 3, soft)
        if (w1 > 0) {
            val lav = Cor.sstep(0.5, 0.95, el) * (1 - Cor.sstep(-2.05, -1.55, az))
            r += w1 * 11.5 * (1 - 0.20 * lav)
            g += w1 * 14.0 * (1 - 0.30 * lav)
            b += w1 * 19.0 * (1 + 0.08 * lav)
        }
        val w2 = janela(az, el, 1.22, 1.86, 0.15, 0.82, 2, 2, soft)
        if (w2 > 0) { r += w2 * 4.2; g += w2 * 4.5; b += w2 * 5.2 }
        o[0] = r; o[1] = g; o[2] = b
    }
}

/** Uma peça desenhada: os pixels e onde ela entra na tela. */
internal class Parte(
    val nome: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val pixels: IntArray
)

/** O retângulo que uma peça ocupa, em pixels, já com espaço para os pixels. */
internal class Caixa(val x: Int, val y: Int, val w: Int, val h: Int) {
    val pixels: IntArray = IntArray(w * h)

    fun parte(nome: String): Parte = Parte(nome, x, y, w, h, pixels)

    companion object {
        /** Recorta a área de desenho ([x0],[y0]) a ([x1],[y1]) em unidades de desenho. */
        fun de(p: Plano, x0: Double, y0: Double, x1: Double, y1: Double): Caixa {
            val xa = maxOf(0, floor(p.ox + x0 * p.s).toInt())
            val xb = minOf(p.largura, Math.ceil(p.ox + x1 * p.s).toInt())
            val ya = maxOf(0, floor(p.oy + y0 * p.s).toInt())
            val yb = minOf(p.altura, Math.ceil(p.oy + y1 * p.s).toInt())
            return Caixa(xa, ya, maxOf(1, xb - xa), maxOf(1, yb - ya))
        }
    }
}

/**
 * De quanto a ampulheta é desenhada: a escala [s] e onde fica o eixo ([ox], [oy]),
 * tudo em pixels da tela.
 */
internal class Plano(val s: Double, val ox: Double, val oy: Double, val largura: Int, val altura: Int) {
    val chave: String = "${largura}x$altura@${"%.5f".format(java.util.Locale.US, s)}"
}

internal fun raiz(v: Double): Double = if (v <= 0.0) 0.0 else sqrt(v)
