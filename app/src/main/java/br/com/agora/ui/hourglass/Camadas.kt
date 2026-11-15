package br.com.agora.ui.hourglass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * As peças que não mudam durante a contagem: os dois discos de madeira, as colunas,
 * os anéis de latão, o vidro e a areia já iluminada dos dois bulbos.
 *
 * Cada peça é desenhada pixel por pixel uma única vez por tamanho de tela e depois
 * fica guardada, porque refazer isso a cada quadro seria lento no celular.
 * Portado do projeto original da ampulheta.
 */
internal class Pincel {
    private val tl = DoubleArray(3)
    private val ec = DoubleArray(3)
    private val al = DoubleArray(3)
    private val cc = DoubleArray(3)
    private val ea = DoubleArray(3)
    private val eb = DoubleArray(3)

    // ---------- madeira
    private fun woodAlb(u: Double, v: Double, seed: Double, o: DoubleArray) {
        val w1 = Ruido.fbm(u * 0.0032 + seed * 3.1, v * 0.018 + seed, 4)
        val w2 = Ruido.vnoise(u * 0.021 + seed, v * 0.11)
        val ring = v * 0.155 + w1 * 8.5 + w2 * 0.7
        val t = ring - Math.floor(ring)
        val late = exp(-((t - 0.8) * (t - 0.8)) / 0.008)
        val fib = Ruido.vnoise(u * 0.11 + seed * 5, v * 1.9)
        val fig = Ruido.fbm(u * 0.006 + 40 + seed, v * 0.045, 3)
        var l = 0.56 + 0.9 * (fig - 0.5) - 0.48 * late + 0.24 * (fib - 0.5)
        val pore = Ruido.vnoise(u * 0.045 + 7, v * 3.1 + seed * 9)
        if (pore > 0.83) l -= (pore - 0.83) * 2.4
        l = Cor.clamp(l, 0.04, 1.15)
        o[0] = 0.015 + 0.27 * l * l
        o[1] = 0.0075 + 0.078 * l.pow(2.2)
        o[2] = 0.003 + 0.030 * l.pow(2.4)
    }

    private fun shadeWood(
        nx: Double, ny: Double, nz: Double, vy: Double, vz: Double,
        al: DoubleArray, spec: Double, coat: Double, o: DoubleArray
    ) {
        G.luz(nx, ny, nz, tl)
        var r = al[0] * tl[0]
        var g = al[1] * tl[1]
        var b = al[2] * tl[2]
        var nv = ny * vy + nz * vz
        if (nv < 0.002) nv = 0.002
        var hx = G.L2[0]; var hy = G.L2[1] + vy; var hz = G.L2[2] + vz
        var hl = 1 / sqrt(hx * hx + hy * hy + hz * hz)
        var nh = (nx * hx + ny * hy + nz * hz) * hl
        val s2 = if (nh > 0) nh.pow(40.0) else 0.0
        hx = G.L1[0]; hy = G.L1[1] + vy; hz = G.L1[2] + vz
        hl = 1 / sqrt(hx * hx + hy * hy + hz * hz)
        nh = (nx * hx + ny * hy + nz * hz) * hl
        val s1 = if (nh > 0) nh.pow(70.0) else 0.0
        r += spec * (0.32 * s2 * G.C2[0] + 0.7 * s1 * G.C1[0])
        g += spec * (0.32 * s2 * G.C2[1] + 0.7 * s1 * G.C1[1])
        b += spec * (0.32 * s2 * G.C2[2] + 0.7 * s1 * G.C1[2])
        if (coat > 0) {
            val f = 0.04 + 0.96 * (1 - nv).pow(5.0)
            Ambiente.env(2 * nv * nx, 2 * nv * ny - vy, 2 * nv * nz - vz, ec)
            r += coat * f * ec[0]; g += coat * f * ec[1]; b += coat * f * ec[2]
        }
        o[0] = r; o[1] = g; o[2] = b
    }

    // ---------- disco de madeira (o de cima e o da base)
    fun disco(p: Plano, isBase: Boolean): Parte {
        val rr0 = G.R
        val t0 = G.T
        val e = 15.0
        val eb0 = 11.0
        val yTop = if (isBase) G.BASE_TOP_Y else G.TOP_Y
        val kF = G.k(yTop)
        val vyF = kF
        val vzF = sqrt(1 - kF * kF)
        fun rad(t: Double): Double {
            var r = rr0
            if (t < e) {
                val q = e - t
                r = rr0 - e + raiz(e * e - q * q)
            } else if (t > t0 - eb0) {
                val q = t - (t0 - eb0)
                r = rr0 - eb0 + raiz(eb0 * eb0 - q * q)
            }
            val gq = (t - t0 * 0.56) / 2.4
            return r - 3 * exp(-gq * gq)
        }
        val b = Caixa.de(p, -rr0 - 2, yTop - rr0 * kF - 2, rr0 + 2, yTop + t0 + rr0 * G.k(yTop + t0) + 2)
        val buf = b.pixels
        val s = p.s
        val iS = 1 / s
        val seed = if (isBase) 3.3 else 7.7
        val rf0 = rr0 - e
        for (j in 0 until b.h) {
            val y = (b.y + j + 0.5 - p.oy) * iS
            val dz = (y - yTop) / kF
            for (i in 0 until b.w) {
                val x = (b.x + i + 0.5 - p.ox) * iS
                val o = j * b.w + i
                val fr = sqrt(x * x + dz * dz)
                if (fr <= rf0 + 1.2 * iS / kF && (fr <= rf0 || dz < 0)) {
                    // ---- face de cima
                    val cov = if (dz < 0) Cor.clamp((rf0 - fr) * kF * s + 0.5, 0.0, 1.0) else 1.0
                    if (cov <= 0) continue
                    woodAlb(x * 0.9, dz * 0.30 + 200, seed + 1.7, al)
                    shadeWood(0.0, 1.0, 0.0, vyF, vzF, al, 0.6, 0.55, cc)
                    if (isBase) {
                        // sulco e sombra em volta do anel, calor refletido da areia e pés das colunas
                        val ao = 1 - 0.62 * exp(-((fr - G.RING_R - 3) / 15).pow(2.0)) -
                            0.25 * exp(-((fr - G.RING_R) / 60).pow(2.0)) * (if (dz < 0) 1.0 else 0.4)
                        var sp = 1.0
                        for (sx in doubleArrayOf(-G.SP_X, G.SP_X)) {
                            val q = (x - sx) * (x - sx) + dz * dz
                            sp *= 1 - 0.65 * exp(-q / 3400.0)
                        }
                        cc[0] *= ao * sp; cc[1] *= ao * sp; cc[2] *= ao * sp
                        if (fr > G.RING_R) {
                            val gl = exp(-((fr - G.RING_R) / 70).pow(2.0)) * 0.55 * Cor.sstep(-120.0, 260.0, dz)
                            cc[0] += gl * 0.55; cc[1] += gl * 0.33; cc[2] += gl * 0.08
                        }
                    }
                    buf[o] = Cor.packLin(cc[0], cc[1], cc[2], cov)
                    continue
                }
                // ---- faixa lateral
                var t = y - yTop - rr0 * G.k(yTop) * raiz(1 - (x / rr0) * (x / rr0))
                var ok = true
                for (it in 0 until 3) {
                    val rr = rad(Cor.clamp(t, 0.0, t0))
                    val sp = x / rr
                    if (sp <= -1 || sp >= 1) { ok = false; break }
                    t = y - yTop - rr * G.k(yTop + t) * sqrt(1 - sp * sp)
                }
                if (!ok || t < -0.5 || t > t0 + 1.5 * iS) continue
                val tt = Cor.clamp(t, 0.0, t0)
                val rr = rad(tt)
                val ax = abs(x)
                val cov = Cor.clamp((rr - ax) * s + 0.5, 0.0, 1.0) * Cor.clamp((t0 - t) * s + 0.5, 0.0, 1.0)
                if (cov <= 0) continue
                val sp = Cor.clamp(x / rr, -1.0, 1.0)
                val cp = raiz(1 - sp * sp)
                val drdt = (rad(minOf(t0, tt + 0.6)) - rad(maxOf(0.0, tt - 0.6))) / 1.2
                var nx = sp; var ny = drdt; var nz = cp
                val il = 1 / sqrt(nx * nx + ny * ny + nz * nz); nx *= il; ny *= il; nz *= il
                val kk = G.k(yTop + tt)
                woodAlb(rr0 * asin(sp), tt + seed * 40, seed, al)
                shadeWood(nx, ny, nz, kk, sqrt(1 - kk * kk), al, 1.0, if (isBase) 0.3 else 0.12, cc)
                val a = if (isBase) 0.82 + 0.18 * Cor.sstep(t0, t0 * 0.55, tt)
                else 0.75 + 0.25 * Cor.sstep(t0, t0 * 0.6, tt)
                cc[0] *= a; cc[1] *= a; cc[2] *= a
                buf[o] = Cor.packLin(cc[0], cc[1], cc[2], cov)
            }
        }
        return b.parte(if (isBase) "discoBase" else "discoTopo")
    }

    // ---------- colunas laterais
    private fun spindleR(s: Double): Double {
        val q = minOf(s, G.GLASS_H - s)
        var r = 30.0
        var col = 40.0
        if (q > 12) {
            val u = Cor.clamp((q - 12) / 4, 0.0, 1.0)
            col = 40 - 8 * (1 - raiz(1 - u * u))
        }
        if (q <= 16) r = maxOf(r, col)
        val bq = (q - 43) / 21
        if (bq > -1 && bq < 1) r = maxOf(r, 30 + 15 * sqrt(1 - bq * bq))
        val rq = (q - 77) / 7
        if (rq > -1 && rq < 1) r = maxOf(r, 30 + 6.5 * sqrt(1 - rq * rq))
        return r
    }

    fun colunas(p: Plano): List<Parte> {
        val out = ArrayList<Parte>(2)
        val s = p.s
        val iS = 1 / s
        for (sx in intArrayOf(-1, 1)) {
            val xc = sx * G.SP_X
            val b = Caixa.de(p, xc - 48, G.TOP_BOT_Y, xc + 48, G.BASE_TOP_Y + 8)
            val buf = b.pixels
            val seed = if (sx < 0) 11.3 else 19.1
            for (j in 0 until b.h) {
                val y = (b.y + j + 0.5 - p.oy) * iS
                val sPos = Cor.clamp(y - G.TOP_BOT_Y, 0.0, G.GLASS_H)
                val r = spindleR(sPos)
                val dr = (spindleR(minOf(G.GLASS_H, sPos + 0.6)) - spindleR(maxOf(0.0, sPos - 0.6))) / 1.2
                val kk = G.k(y)
                val vy = kk
                val vz = sqrt(1 - kk * kk)
                for (i in 0 until b.w) {
                    val x = (b.x + i + 0.5 - p.ox) * iS - xc
                    val ax = abs(x)
                    var cov = Cor.clamp((r - ax) * s + 0.5, 0.0, 1.0)
                    if (cov <= 0) continue
                    if (y > G.BASE_TOP_Y - 1) {
                        val yEnd = G.BASE_TOP_Y + raiz(r * r - x * x) * G.k(G.BASE_TOP_Y)
                        cov *= Cor.clamp((yEnd - y) * s + 0.5, 0.0, 1.0)
                        if (cov <= 0) continue
                    }
                    val sp = Cor.clamp(x / r, -1.0, 1.0)
                    val cp = raiz(1 - sp * sp)
                    var nx = sp; var ny = dr; var nz = cp
                    val il = 1 / sqrt(nx * nx + ny * ny + nz * nz); nx *= il; ny *= il; nz *= il
                    woodAlb(sPos * 1.0 + seed * 50, 30 * asin(sp) + seed * 13, seed, al)
                    shadeWood(nx, ny, nz, vy, vz, al, 1.0, 0.8, cc)
                    val ao = 0.70 + 0.30 * Cor.sstep(0.0, 70.0, minOf(sPos, G.GLASS_H - sPos))
                    buf[j * b.w + i] = Cor.packLin(cc[0] * ao, cc[1] * ao, cc[2] * ao, cov)
                }
            }
            out.add(b.parte(if (sx < 0) "colunaEsq" else "colunaDir"))
        }
        return out
    }

    // ---------- anéis de latão
    fun anel(p: Plano, isTop: Boolean): Parte {
        val h = G.RING_H
        val rr = G.RING_R
        val y0 = if (isTop) G.TOP_BOT_Y else G.BASE_TOP_Y - h
        fun rad(t: Double): Double = rr - 7 * (1 - Math.sin(PI * Cor.clamp(t / h, 0.0, 1.0)))
        val b = Caixa.de(p, -rr - 2, y0 - rr * G.k(y0) - 2, rr + 2, y0 + h + rr * G.k(y0 + h) + 2)
        val buf = b.pixels
        val s = p.s
        val iS = 1 / s
        val f0 = doubleArrayOf(0.80, 0.56, 0.24)
        for (j in 0 until b.h) {
            val y = (b.y + j + 0.5 - p.oy) * iS
            for (i in 0 until b.w) {
                val x = (b.x + i + 0.5 - p.ox) * iS
                var t = y - y0 - rr * G.k(y0) * raiz(1 - (x / rr) * (x / rr))
                var ok = true
                for (it in 0 until 3) {
                    val r2 = rad(Cor.clamp(t, 0.0, h))
                    val sp = x / r2
                    if (sp <= -1 || sp >= 1) { ok = false; break }
                    t = y - y0 - r2 * G.k(y0 + t) * sqrt(1 - sp * sp)
                }
                if (!ok || t < -1.5 * iS || t > h + 1.5 * iS) continue
                val tt = Cor.clamp(t, 0.0, h)
                val r2 = rad(tt)
                val cov = Cor.clamp((r2 - abs(x)) * s + 0.5, 0.0, 1.0) *
                    Cor.clamp((h - t) * s + 0.5, 0.0, 1.0) * Cor.clamp(t * s + 0.5, 0.0, 1.0)
                if (cov <= 0) continue
                val sp = Cor.clamp(x / r2, -1.0, 1.0)
                val cp = raiz(1 - sp * sp)
                val drdt = rad(minOf(h, tt + 0.5)) - rad(maxOf(0.0, tt - 0.5))
                var nx = sp; var ny = drdt; var nz = cp
                val il = 1 / sqrt(nx * nx + ny * ny + nz * nz); nx *= il; ny *= il; nz *= il
                val kk = G.k(y0 + tt)
                val vy = kk
                val vz = sqrt(1 - kk * kk)
                var nv = ny * vy + nz * vz
                if (nv < 0.002) nv = 0.002
                Ambiente.env(2 * nv * nx, 2 * nv * ny - vy, 2 * nv * nz - vz, ec, true)
                G.luz(nx, ny, nz, tl)
                val f5 = (1 - nv).pow(5.0)
                val occ = if (isTop) 0.55 + 0.45 * Cor.sstep(0.0, h, tt) else 1.0
                val rr0 = f0[0] + (1 - f0[0]) * f5
                val rg0 = f0[1] + (1 - f0[1]) * f5
                val rb0 = f0[2] + (1 - f0[2]) * f5
                buf[j * b.w + i] = Cor.packLin(
                    occ * (rr0 * ec[0] * 0.9 + 0.10 * f0[0] * tl[0]),
                    occ * (rg0 * ec[1] * 0.9 + 0.10 * f0[1] * tl[1]),
                    occ * (rb0 * ec[2] * 0.9 + 0.10 * f0[2] * tl[2]),
                    cov
                )
            }
        }
        return b.parte(if (isTop) "anelTopo" else "anelBase")
    }

    // ---------- vidro
    fun vidroFrente(p: Plano): Parte {
        val b = Caixa.de(p, -G.R_MAX - 3, G.TOP_BOT_Y + 6, G.R_MAX + 3, G.BASE_TOP_Y + 2)
        val buf = b.pixels
        val s = p.s
        val iS = 1 / s
        for (j in 0 until b.h) {
            val y = (b.y + j + 0.5 - p.oy) * iS
            val top = y < G.NECK_Y
            val d = abs(y - G.NECK_Y)
            val ro = G.rOut(d)
            val ri = G.rIn(d)
            val slope = G.dROut(d) * (if (top) -1 else 1)
            val kk = G.k(y)
            val vy = kk
            val vz = sqrt(1 - kk * kk)
            for (i in 0 until b.w) {
                val x = (b.x + i + 0.5 - p.ox) * iS
                val ax = if (x < 0) -x else x
                if (ax > ro + iS) continue
                val cov = Cor.clamp((ro - ax) * s + 0.5, 0.0, 1.0)
                var sp = x / ro
                sp = if (sp > 1) 1.0 else if (sp < -1) -1.0 else sp
                val cp = raiz(1 - sp * sp)
                var nx = sp; var ny = slope; var nz = cp
                var il = 1 / sqrt(nx * nx + ny * ny + nz * nz); nx *= il; ny *= il; nz *= il
                var nv = ny * vy + nz * vz
                if (nv < 0.002) nv = 0.002
                val f = 0.045 + 0.955 * (1 - nv).pow(5.0)
                Ambiente.env(2 * nv * nx, 2 * nv * ny - vy, 2 * nv * nz - vz, ea)
                val steep = Cor.sstep(2.2, 0.8, abs(slope))
                var r = f * ea[0] * steep
                var g = f * ea[1] * steep
                var bl = f * ea[2] * steep
                if (steep < 1) {
                    val hx = G.L1[0]; val hy = G.L1[1] + vy; val hz = G.L1[2] + vz
                    val hl = 1 / sqrt(hx * hx + hy * hy + hz * hz)
                    val nh = maxOf(0.0, (nx * hx + ny * hy + nz * hz) * hl)
                    val sh = (1 - steep) * (0.25 * nh.pow(6.0) + 0.5 * nh.pow(40.0))
                    r += sh * 1.1; g += sh * 1.05; bl += sh * 1.0
                }
                var a = 0.02
                if (ax < ri) {
                    val spi = x / ri
                    val cpi = raiz(1 - spi * spi)
                    var mx = spi; var my = slope; var mz = cpi
                    il = 1 / sqrt(mx * mx + my * my + mz * mz); mx *= il; my *= il; mz *= il
                    var nvi = my * vy + mz * vz
                    if (nvi < 0.002) nvi = 0.002
                    val fi = 0.045 + 0.955 * (1 - nvi).pow(5.0)
                    Ambiente.env(2 * nvi * mx, 2 * nvi * my - vy, 2 * nvi * mz - vz, eb)
                    r += 0.45 * fi * eb[0] * steep
                    g += 0.45 * fi * eb[1] * steep
                    bl += 0.45 * fi * eb[2] * steep
                } else {
                    val w = Cor.clamp((ax - ri) / maxOf(0.5, ro - ri), 0.0, 1.0)
                    a = 0.20 + 0.28 * w
                    r += 0.012; g += 0.009; bl += 0.007
                }
                val ld = (ax - ri) * s
                val line = exp(-(ld * ld) / 1.6)
                r += line * 0.12; g += line * 0.11; bl += line * 0.10
                a = maxOf(a, line * 0.3)
                val pr = Cor.enc(Cor.aces(r))
                val pg = Cor.enc(Cor.aces(g))
                val pb = Cor.enc(Cor.aces(bl))
                var alfa = minOf(1.0, a + 0.9 * maxOf(pr, maxOf(pg, pb)))
                val cr = pr + 0.02 * a
                val cg = pg + 0.014 * a
                val cb = pb + 0.01 * a
                alfa = maxOf(alfa, maxOf(cr, maxOf(cg, maxOf(cb, 0.0001))))
                buf[j * b.w + i] = Cor.pack(cr / alfa, cg / alfa, cb / alfa, alfa * cov)
            }
        }
        return b.parte("vidroFrente")
    }

    fun vidroTras(p: Plano): Parte {
        val b = Caixa.de(p, -G.R_MAX - 3, G.TOP_BOT_Y + 6, G.R_MAX + 3, G.BASE_TOP_Y + 2)
        val buf = b.pixels
        val s = p.s
        val iS = 1 / s
        for (j in 0 until b.h) {
            val y = (b.y + j + 0.5 - p.oy) * iS
            val top = y < G.NECK_Y
            val d = abs(y - G.NECK_Y)
            val ri = G.rIn(d)
            val slope = G.dROut(d) * (if (top) -1 else 1)
            val kk = G.k(y)
            val vy = kk
            val vz = sqrt(1 - kk * kk)
            for (i in 0 until b.w) {
                val x = (b.x + i + 0.5 - p.ox) * iS
                val ax = if (x < 0) -x else x
                if (ax > ri + iS) continue
                val cov = Cor.clamp((ri - ax) * s + 0.5, 0.0, 1.0)
                val sp = Cor.clamp(x / ri, -1.0, 1.0)
                val cp = raiz(1 - sp * sp)
                var nx = -sp; var ny = -slope; var nz = cp
                val il = 1 / sqrt(nx * nx + ny * ny + nz * nz); nx *= il; ny *= il; nz *= il
                var nv = ny * vy + nz * vz
                if (nv < 0.002) nv = 0.002
                val f = 0.045 + 0.955 * (1 - nv).pow(5.0)
                Ambiente.env(2 * nv * nx, 2 * nv * ny - vy, 2 * nv * nz - vz, ea)
                val r = Cor.enc(Cor.aces(0.10 * f * ea[0]))
                val g = Cor.enc(Cor.aces(0.10 * f * ea[1]))
                val bl = Cor.enc(Cor.aces(0.10 * f * ea[2]))
                val wr = r + 0.085
                val wg = g + 0.055
                val wb = bl + 0.032
                val alfa = maxOf(0.13 + 0.6 * maxOf(r, maxOf(g, bl)), maxOf(wr, maxOf(wg, wb)))
                buf[j * b.w + i] = Cor.pack(wr / alfa, wg / alfa, wb / alfa, alfa * cov)
            }
        }
        return b.parte("vidroTras")
    }
}
