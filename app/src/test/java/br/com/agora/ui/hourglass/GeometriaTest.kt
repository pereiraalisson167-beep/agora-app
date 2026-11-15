package br.com.agora.ui.hourglass

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Confere a forma da ampulheta contra os números do projeto original.
 *
 * Os valores esperados foram tirados do perfil de vidro do protótipo, calculado à parte.
 * Se alguém mexer nas curvas sem querer, estes testes reprovam a rodada.
 */
class GeometriaTest {

    private val folga = 1e-4

    @Test
    fun `as medidas principais batem com o original`() {
        assertEquals(728.0, G.NECK_Y, folga)
        assertEquals(1298.0, G.BASE_TOP_Y, folga)
        assertEquals(1416.0, G.BASE_BOT_Y, folga)
        assertEquals(570.0, G.L, folga)
        assertEquals(0.040, G.k(G.TOP_Y), folga)
        assertEquals(0.115, G.k(G.BASE_TOP_Y), folga)
    }

    @Test
    fun `o perfil do vidro bate ponto a ponto`() {
        assertEquals(21.000000, G.rOut(0.0), 1e-5)
        assertEquals(68.556380, G.rOut(50.0), 1e-5)
        assertEquals(141.378265, G.rOut(100.0), 1e-5)
        assertEquals(265.304611, G.rOut(200.0), 1e-5)
        assertEquals(338.652024, G.rOut(300.0), 1e-5)
        assertEquals(362.000000, G.rOut(400.0), 1e-5)
        assertEquals(354.921451, G.rOut(500.0), 1e-5)
        assertEquals(336.000000, G.rOut(569.0), 1e-5)
    }

    @Test
    fun `a parede e mais grossa junto ao gargalo`() {
        assertEquals(13.0, G.th(0.0), folga)
        assertTrue(G.th(0.0) > G.th(300.0))
        assertEquals(8.0, G.rIn(0.0), 1e-5)
        assertEquals(327.0, G.R_FLOOR, 1e-5)
    }

    @Test
    fun `o volume interno bate com o original`() {
        assertEquals(1_667_477.784, G.volume(100.0), 0.01)
        assertEquals(42_322_124.436, G.volume(300.0), 0.01)
        assertEquals(80_576_185.973, G.V_TOT, 0.01)
        assertEquals(19_040_362.813, G.V1_MAX, 0.01)
    }

    @Test
    fun `achar a distancia pelo volume desfaz a conta do volume`() {
        for (d in listOf(10.0, 120.0, 250.0, 390.0)) {
            assertEquals(d, G.dDeVolume(G.volume(d)), 0.5)
        }
    }

    @Test
    fun `no comeco a areia esta toda em cima e no fim toda embaixo`() {
        val comeco = G.estadoDaAreia(0.0)
        assertEquals(G.D_FULL, comeco.ds, 1.0)
        assertEquals(0, comeco.fase)
        assertEquals(0.0, comeco.hApex, folga)

        val fim = G.estadoDaAreia(1.0)
        assertEquals(0.0, fim.ds, folga)
        assertEquals(2, fim.fase)
        assertTrue(fim.hApex > 0)
        assertTrue("o monte nao pode passar do gargalo", fim.yApex > G.NECK_Y)
    }

    @Test
    fun `a areia de cima so desce e o monte so sobe`() {
        var dsAnterior = Double.MAX_VALUE
        var apiceAnterior = -1.0
        var p = 0.0
        while (p <= 1.0001) {
            val st = G.estadoDaAreia(p)
            assertTrue("a areia de cima subiu em p=$p", st.ds <= dsAnterior + 1e-9)
            assertTrue("o monte baixou em p=$p", st.hApex >= apiceAnterior - 1e-9)
            dsAnterior = st.ds
            apiceAnterior = st.hApex
            p += 0.005
        }
    }

    @Test
    fun `o monte troca de fase quando enche o fundo`() {
        val limite = G.V1_MAX / G.V_TOT
        assertEquals(1, G.estadoDaAreia(limite * 0.9).fase)
        assertEquals(2, G.estadoDaAreia(limite * 1.1).fase)
    }

    @Test
    fun `a altura do monte e maxima no eixo`() {
        val st = G.estadoDaAreia(0.8)
        assertEquals(st.yApex, G.alturaDoMonte(0.0, st), folga)
        assertTrue(G.alturaDoMonte(100.0, st) > G.alturaDoMonte(0.0, st))
        assertTrue(G.alturaDoMonte(400.0, st) >= st.yf - folga)
        assertEquals(G.BASE_TOP_Y, G.alturaDoMonte(0.0, G.estadoDaAreia(0.0)), folga)
    }

    @Test
    fun `a luz devolve cor em todas as direcoes`() {
        val saida = DoubleArray(3)
        for (direcao in listOf(
            doubleArrayOf(0.0, 1.0, 0.0), doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0), doubleArrayOf(0.0, -1.0, 0.0)
        )) {
            G.luz(direcao[0], direcao[1], direcao[2], saida)
            assertTrue(saida.all { it > 0.0 })
        }
    }
}
