package br.com.agora.ui.hourglass

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A forma da ampulheta, em unidades de desenho: o diâmetro do disco vale 1000.
 * x = 0 no eixo; y cresce para baixo.
 *
 * Portado do projeto original da ampulheta, mantendo as mesmas medidas, o mesmo
 * perfil de vidro e o mesmo cálculo de volume, para a areia subir e descer igual.
 */
object G {
    const val R = 500.0                 // raio do disco
    const val T = 118.0                 // espessura do disco
    const val TOP_Y = 40.0              // disco de cima: centro da face superior
    const val TOP_BOT_Y = TOP_Y + T     // disco de cima: face inferior (topo do vidro)
    const val GLASS_H = 1140.0
    const val L = GLASS_H / 2           // meia altura do vidro
    const val NECK_Y = TOP_BOT_Y + L    // o gargalo
    const val BASE_TOP_Y = TOP_BOT_Y + GLASS_H
    const val BASE_BOT_Y = BASE_TOP_Y + T
    const val K_TOP = 0.040             // achatamento da elipse no topo...
    const val K_BASE = 0.115            // ...e na base
    const val SP_X = 421.0              // distância das colunas até o eixo
    const val RING_R = 352.0            // anéis de latão nas duas pontas do vidro
    const val RING_H = 30.0
    const val TAN_T = 0.52              // inclinação do monte de areia (~27,5 graus)
    const val R_MAX = 362.0
    const val BOX_W = 1100.0

    /** Achatamento da elipse na altura [y]: a câmera está um pouco acima. */
    fun k(y: Double): Double = K_TOP + (K_BASE - K_TOP) * (y - TOP_Y) / (BASE_TOP_Y - TOP_Y)

    val BOX_TOP: Double = TOP_Y - R * k(TOP_Y) - 12
    val BOX_BOT: Double = BASE_BOT_Y + R * k(BASE_BOT_Y) + 34
    val BOX_H: Double = BOX_BOT - BOX_TOP

    // ---- perfil do vidro: o raio externo em função de d, a distância até o gargalo
    private val N: Int = Math.ceil(L).toInt() + 2
    private val rO = DoubleArray(N)

    init {
        val segs = arrayOf(
            arrayOf(doubleArrayOf(0.0, 21.0), doubleArrayOf(55.0, 21.0), doubleArrayOf(175.0, 362.0), doubleArrayOf(400.0, 362.0)),
            arrayOf(doubleArrayOf(400.0, 362.0), doubleArrayOf(470.0, 362.0), doubleArrayOf(535.0, 353.0), doubleArrayOf(562.0, 336.0))
        )
        val pts = ArrayList<DoubleArray>(1404)
        for (s in segs) {
            for (i in 0..700) {
                val t = i / 700.0
                val u = 1 - t
                val a = u * u * u
                val b = 3 * u * u * t
                val c = 3 * u * t * t
                val d = t * t * t
                pts.add(
                    doubleArrayOf(
                        a * s[0][0] + b * s[1][0] + c * s[2][0] + d * s[3][0],
                        a * s[0][1] + b * s[1][1] + c * s[2][1] + d * s[3][1]
                    )
                )
            }
        }
        pts.add(doubleArrayOf(L + 2, 336.0))
        var j = 0
        for (d in 0 until N) {
            while (j < pts.size - 2 && pts[j + 1][0] < d) j++
            val a = pts[j]
            val b = pts[j + 1]
            val t = if (b[0] > a[0]) min(1.0, max(0.0, (d - a[0]) / (b[0] - a[0]))) else 0.0
            rO[d] = a[1] + (b[1] - a[1]) * t
        }
    }

    /** Raio externo do vidro a [d] do gargalo. */
    fun rOut(dIn: Double): Double {
        var d = if (dIn < 0) -dIn else dIn
        if (d > L) d = L
        val i = min(N - 2, d.toInt())
        val f = d - i
        return rO[i] + (rO[i + 1] - rO[i]) * f
    }

    /** Quanto o raio externo muda ali: serve para a inclinação da superfície. */
    fun dROut(d: Double): Double {
        val a = max(0.0, d - 0.6)
        val b = d + 0.6
        return (rOut(b) - rOut(a)) / (b - a)
    }

    /** Espessura do vidro: mais grossa junto ao gargalo. */
    fun th(d: Double): Double = 9 + 4 * Math.exp(-(d * d) / 1600.0)

    fun rIn(d: Double): Double = rOut(d) - th(d)

    /** Raio usado para desenhar a areia, um fio dentro do vidro. */
    fun rDraw(d: Double): Double = rOut(d) - 1.5

    // ---- volume interno acumulado desde o gargalo, para o nível da areia ser de verdade
    private val V = DoubleArray(N)

    init {
        for (d in 1 until N) {
            val r0 = rIn((d - 1).toDouble())
            val r1 = rIn(d.toDouble())
            V[d] = V[d - 1] + PI * (r0 * r0 + r1 * r1) / 2
        }
    }

    fun volume(d: Double): Double {
        if (d <= 0) return 0.0
        if (d >= N - 1) return V[N - 1]
        val i = d.toInt()
        val f = d - i
        return V[i] + (V[i + 1] - V[i]) * f
    }

    /** O caminho inverso: a que distância do gargalo está o volume [v]. */
    fun dDeVolume(v: Double): Double {
        if (v <= 0) return 0.0
        var lo = 0
        var hi = N - 1
        while (hi - lo > 1) {
            val m = (lo + hi) ushr 1
            if (V[m] < v) lo = m else hi = m
        }
        val f = (v - V[lo]) / max(1e-9, V[hi] - V[lo])
        return lo + min(1.0, max(0.0, f))
    }

    const val D_FULL = 402.0
    val V_TOT: Double = volume(D_FULL)
    val R_FLOOR: Double = rIn(L)
    val V1_MAX: Double = PI * R_FLOOR * R_FLOOR * R_FLOOR * TAN_T / 3

    private fun fillVol(h: Double): Double = volume(L) - volume(L - h)

    private fun pileVol(h: Double): Double {
        val r = rIn(L - h)
        return fillVol(h) + PI * r * r * r * TAN_T / 3
    }

    /**
     * Onde está a areia quando o tempo da tarefa já correu [p] (de 0 a 1).
     *
     * Em cima: um corpo de areia que vai sumindo. Embaixo: um cone que cresce
     * (fase 1) e depois um cone sobre uma camada cheia (fase 2).
     */
    fun estadoDaAreia(pIn: Double): EstadoAreia {
        val p = min(1.0, max(0.0, pIn))
        val vt = (1 - p) * V_TOT
        val vb = p * V_TOT
        var ds = 0.0
        var ys = 0.0
        var rs = 0.0
        var ks = 0.0
        if (vt > 2) {
            ds = dDeVolume(vt)
            ys = NECK_Y - ds
            rs = rDraw(ds)
            ks = k(ys)
        }
        var fase = 0
        var a = 0.0
        var hApex = 0.0
        var hf = 0.0
        var yf = 0.0
        var rf = 0.0
        var kf = 0.0
        if (vb <= 2) {
            fase = 0
        } else if (vb <= V1_MAX) {
            fase = 1
            a = cbrt(3 * vb / (PI * TAN_T))
            hApex = a * TAN_T
        } else {
            fase = 2
            var lo = 0.0
            var hi = L - 40
            for (i in 0 until 30) {
                val m = (lo + hi) / 2
                if (pileVol(m) < vb) lo = m else hi = m
            }
            hf = (lo + hi) / 2
            yf = BASE_TOP_Y - hf
            rf = rDraw(L - hf)
            kf = k(yf)
            hApex = hf + rIn(L - hf) * TAN_T
        }
        return EstadoAreia(
            p = p, ds = ds, ys = ys, rs = rs, ks = ks,
            fase = fase, a = a, hf = hf, yf = yf, rf = rf, kf = kf,
            hApex = hApex, yApex = BASE_TOP_Y - hApex
        )
    }

    // ---- luzes (x para a direita, y para cima, z na direção da câmera)
    private fun nrm(x: Double, y: Double, z: Double): DoubleArray {
        val l = hypot(hypot(x, y), z)
        return doubleArrayOf(x / l, y / l, z / l)
    }

    val L1: DoubleArray = nrm(-0.82, 0.42, -0.30)   // luz da janela: à esquerda, um pouco atrás (fria)
    val L2: DoubleArray = nrm(-0.44, 0.52, 0.73)    // luz principal quente, da frente à esquerda
    val L3: DoubleArray = nrm(0.80, 0.16, -0.58)    // contorno quente do fundo aceso (direita)
    val C1 = doubleArrayOf(0.46, 0.48, 0.53)
    val C2 = doubleArrayOf(1.08, 0.88, 0.66)
    val C3 = doubleArrayOf(0.62, 0.36, 0.15)
    val AMB = doubleArrayOf(0.075, 0.055, 0.04)

    /** A cor da luz que bate numa superfície virada para [nx], [ny], [nz]. */
    fun luz(nx: Double, ny: Double, nz: Double, out: DoubleArray): DoubleArray {
        val d1 = max(0.0, nx * L1[0] + ny * L1[1] + nz * L1[2])
        val d2 = max(0.0, nx * L2[0] + ny * L2[1] + nz * L2[2])
        val d3 = max(0.0, nx * L3[0] + ny * L3[1] + nz * L3[2])
        out[0] = AMB[0] + C1[0] * d1 + C2[0] * d2 + C3[0] * d3
        out[1] = AMB[1] + C1[1] * d1 + C2[1] * d2 + C3[1] * d3
        out[2] = AMB[2] + C1[2] * d1 + C2[2] * d2 + C3[2] * d3
        return out
    }

    val ANG_T: Double = atan(TAN_T)
    val COS_T: Double = Math.cos(ANG_T)
    val SIN_T: Double = Math.sin(ANG_T)

    /** A altura do monte embaixo de x, para a areia que cai saber onde parar. */
    fun alturaDoMonte(x: Double, st: EstadoAreia): Double {
        if (st.fase == 0) return BASE_TOP_Y
        val ax = abs(x)
        if (st.fase == 1) return if (ax < st.a) st.yApex + ax * TAN_T else BASE_TOP_Y
        return if (ax < st.rf) st.yApex + ax * TAN_T else st.yf
    }

    fun raizSegura(v: Double): Double = if (v <= 0.0) 0.0 else sqrt(v)
}

/** O retrato da areia em um instante. */
data class EstadoAreia(
    val p: Double,
    val ds: Double,
    val ys: Double,
    val rs: Double,
    val ks: Double,
    val fase: Int,
    val a: Double,
    val hf: Double,
    val yf: Double,
    val rf: Double,
    val kf: Double,
    val hApex: Double,
    val yApex: Double
)
