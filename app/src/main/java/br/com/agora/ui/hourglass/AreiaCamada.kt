package br.com.agora.ui.hourglass

import kotlin.math.abs
import kotlin.math.pow

/**
 * A areia dos dois bulbos, já iluminada, mais o pedacinho de grãos que se repete.
 * O desenho de cada quadro apenas recorta estas imagens no nível certo.
 * Portado do projeto original da ampulheta.
 */
internal object AreiaCamada {

    private const val TS = 160

    /** O quadradinho de grãos que se repete por toda a areia. */
    fun grao(p: Plano): Parte {
        val gr = Cor.clamp(1.45 * p.s, 0.8, 2.6)
        val r = Mulberry32(4242)
        val f = FloatArray(TS * TS * 3)
        val per = 5
        val lat = DoubleArray(per * per)
        for (i in lat.indices) lat[i] = r.next()

        fun pn(x: Int, y: Int): Double {
            val fx = x.toDouble() / TS * per
            val fy = y.toDouble() / TS * per
            val xi = Math.floor(fx).toInt()
            val yi = Math.floor(fy).toInt()
            val u = fx - xi
            val v = fy - yi
            val su = u * u * (3 - 2 * u)
            val sv = v * v * (3 - 2 * v)
            val x0 = xi % per; val x1 = (xi + 1) % per
            val y0 = yi % per; val y1 = (yi + 1) % per
            val a = lat[y0 * per + x0]; val b = lat[y0 * per + x1]
            val c = lat[y1 * per + x0]; val d = lat[y1 * per + x1]
            return a + (b - a) * su + (c - a) * sv + (a - b - c + d) * su * sv
        }

        for (y in 0 until TS) for (x in 0 until TS) {
            val k = 0.87 + 0.10 * pn(x, y) + 0.06 * (r.next() - 0.5)
            val i = (y * TS + x) * 3
            f[i] = (1.0 * k).toFloat()
            f[i + 1] = (0.76 * k).toFloat()
            f[i + 2] = (0.22 * k).toFloat()
        }

        // grãos soltos de tons diferentes, espalhados por cima
        val pal = arrayOf(
            doubleArrayOf(0.66, 0.38, 0.06, 0.02),
            doubleArrayOf(0.86, 0.58, 0.12, 0.15),
            doubleArrayOf(0.98, 0.76, 0.22, 0.46),
            doubleArrayOf(1.0, 0.86, 0.36, 0.33),
            doubleArrayOf(1.0, 0.96, 0.76, 0.04)
        )
        val quantos = Math.round(TS.toDouble() * TS / (Math.PI * gr * gr) * 1.55).toInt()
        for (n in 0 until quantos) {
            val cx = r.next() * TS
            val cy = r.next() * TS
            val rr = gr * (0.55 + r.next() * 0.9)
            var pSorteio = r.next()
            var col = pal[2]
            for (c in pal) {
                if (pSorteio < c[3]) { col = c; break }
                pSorteio -= c[3]
            }
            val x0 = Math.floor(cx - rr - 1).toInt()
            val x1 = Math.ceil(cx + rr + 1).toInt()
            val y0 = Math.floor(cy - rr - 1).toInt()
            val y1 = Math.ceil(cy + rr + 1).toInt()
            for (yy in y0..y1) for (xx in x0..x1) {
                val dx = xx + 0.5 - cx
                val dy = yy + 0.5 - cy
                val q = (dx * dx + dy * dy) / (rr * rr)
                if (q >= 1) continue
                val sh = 0.80 + 0.32 * Cor.clamp((-dx - dy) / (rr * 1.5), -1.0, 1.0) * (1 - q) + 0.08 * (1 - q)
                val a = minOf(1.0, (1 - q) * 3)
                val px = ((xx % TS) + TS) % TS
                val py = ((yy % TS) + TS) % TS
                val i = (py * TS + px) * 3
                f[i] += ((col[0] * sh - f[i]) * a).toFloat()
                f[i + 1] += ((col[1] * sh - f[i + 1]) * a).toFloat()
                f[i + 2] += ((col[2] * sh - f[i + 2]) * a).toFloat()
            }
        }

        val pixels = IntArray(TS * TS)
        for (i in 0 until TS * TS) {
            pixels[i] = Cor.pack(f[i * 3].toDouble(), f[i * 3 + 1].toDouble(), f[i * 3 + 2].toDouble(), 1.0)
        }
        return Parte("grao", 0, 0, TS, TS, pixels)
    }

    /**
     * A areia de um bulbo, pintada inteira. O brilho muda da esquerda para a direita e
     * de cima para baixo, como se a luz entrasse pela frente.
     */
    fun bulbo(p: Plano, grao: Parte, isTop: Boolean): Parte {
        val y0 = if (isTop) G.NECK_Y - G.L + 8 else G.NECK_Y - 4
        val y1 = if (isTop) G.NECK_Y + 4 else G.BASE_TOP_Y + G.R_FLOOR * G.k(G.BASE_TOP_Y) + 3
        val b = Caixa.de(p, -G.R_MAX - 2, y0, G.R_MAX + 2, y1)
        val buf = b.pixels
        val tile = grao.pixels
        val s = p.s
        val iS = 1 / s
        val kB = G.k(G.BASE_TOP_Y)
        for (j in 0 until b.h) {
            val yPix = b.y + j
            val y = (yPix + 0.5 - p.oy) * iS
            val d = minOf(G.L, abs(y - G.NECK_Y))
            val rd = G.rDraw(d)
            val ro = G.rOut(d)
            val vmul = if (isTop) 1.0 + 0.45 * (1 - Cor.sstep(4.0, 90.0, G.NECK_Y - y))
            else 0.88 + 0.12 * Cor.sstep(0.0, 70.0, G.BASE_TOP_Y - y)
            for (i in 0 until b.w) {
                val xPix = b.x + i
                val x = (xPix + 0.5 - p.ox) * iS
                val ax = abs(x)
                val cov: Double
                if (!isTop && y > G.BASE_TOP_Y) {
                    val rf = G.rDraw(G.L)
                    val q = (x * x) / (rf * rf) + ((y - G.BASE_TOP_Y) / (rf * kB)).pow(2.0)
                    if (q >= 1) continue
                    cov = Cor.clamp((1 - q) * rf * kB * s * 0.5, 0.0, 1.0)
                } else {
                    cov = Cor.clamp((rd - ax) * s + 0.5, 0.0, 1.0)
                    if (cov <= 0) continue
                }
                val sp = Cor.clamp(x / ro, -1.0, 1.0)
                var f = if (isTop) 0.42 + 0.40 * sp * sp - 0.12 * sp else 0.40 + 0.34 * sp * sp - 0.10 * sp
                f *= vmul
                val ti = (yPix % TS) * TS + (xPix % TS)
                val cor = tile[ti]
                val tr = (cor shr 16) and 0xFF
                val tg = (cor shr 8) and 0xFF
                val tb = cor and 0xFF
                buf[j * b.w + i] = Cor.pack(
                    tr / 255.0 * minOf(1.0, f.pow(0.75)),
                    tg / 255.0 * minOf(1.0, f.pow(1.25)),
                    tb / 255.0 * minOf(1.0, f.pow(2.4)),
                    cov
                )
            }
        }
        return b.parte(if (isTop) "areiaTopo" else "areiaBase")
    }
}
