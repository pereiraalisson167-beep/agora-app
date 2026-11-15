package br.com.agora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confere que os passos da data não deixam montar uma data impossível ou já passada. */
class DatasTest {

    @Test
    fun `fevereiro aceita o dia 29 e recusa o 30`() {
        assertTrue(mesServe(29, 2))
        assertFalse(mesServe(30, 2))
        assertFalse(mesServe(31, 4))
        assertTrue(mesServe(31, 5))
        assertTrue(mesServe(null, 2))
    }

    @Test
    fun `ano sem 29 de fevereiro e recusado`() {
        val agora = instanteDe(1, 1, 2026, 0, 0)
        assertFalse(anoServe(29, 2, 2027, agora))
        assertTrue(anoServe(29, 2, 2028, agora))
    }

    @Test
    fun `ano que ja passou e recusado`() {
        val agora = instanteDe(10, 6, 2026, 12, 0)
        assertFalse(anoServe(1, 1, 2026, agora))
        assertTrue(anoServe(31, 12, 2026, agora))
    }

    @Test
    fun `hora e minuto que ja passaram sao recusados`() {
        val agora = instanteDe(10, 6, 2026, 14, 30)
        assertFalse(horaServe(10, 6, 2026, 10, agora))
        assertTrue(horaServe(10, 6, 2026, 20, agora))
        assertFalse(minutoServe(10, 6, 2026, 14, 0, agora))
        assertTrue(minutoServe(10, 6, 2026, 14, 55, agora))
    }

    @Test
    fun `dias no mes`() {
        assertEquals(29, diasNoMes(2, 2028))
        assertEquals(28, diasNoMes(2, 2027))
        assertEquals(30, diasNoMes(4, 2026))
        assertEquals(31, diasNoMes(12, 2026))
    }

    @Test
    fun `o tempo que falta aparece em dias horas minutos e segundos`() {
        assertEquals("00:00:00", formatarFalta(0))
        assertEquals("00:00:00", formatarFalta(-5000))
        assertEquals("00:01:05", formatarFalta(65_000))
        assertEquals("02:00:00", formatarFalta(7_200_000))
        assertEquals("1d 00:00:01", formatarFalta(86_401_000))
    }

    @Test
    fun `o resumo mostra tracos no que ainda nao foi escolhido`() {
        assertEquals("--/--/---- · --:--", resumoDaData(null, null, null, null, null))
        assertEquals("05/09/2026 · 08:30", resumoDaData(5, 9, 2026, 8, 30))
    }

    @Test
    fun `a data escrita traz o dia da semana e so mostra o ano quando ele muda`() {
        val agora = instanteDe(1, 1, 2026, 0, 0)
        val mesmoAno = instanteDe(18, 11, 2026, 9, 5)
        assertEquals("qua, 18/11 às 09:05", formatarQuando(mesmoAno, agora))
        val outroAno = instanteDe(18, 11, 2027, 9, 5)
        assertTrue(formatarQuando(outroAno, agora).contains("/2027"))
    }
}
