package br.com.agora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TarefaTest {

    private val agora = 1_700_000_000_000L

    @Test
    fun `dias esperando conta dias inteiros e nunca fica negativo`() {
        val base = Tarefa(titulo = "x", minutos = 5, energia = 1, criadaEm = agora)
        assertEquals(0, base.diasEsperando(agora))
        assertEquals(0, base.diasEsperando(agora - 10_000))
        assertEquals(1, base.copy(criadaEm = agora - 90_000_000).diasEsperando(agora))
        assertEquals(3, base.copy(criadaEm = agora - 3 * 86_400_000L).diasEsperando(agora))
    }

    @Test
    fun `tarefa com data esta aguardando so antes da hora`() {
        val t = Tarefa(titulo = "x", minutos = 5, energia = 1, criadaEm = agora, em = agora + 1000)
        assertTrue(t.aguardandoData(agora))
        assertFalse(t.aguardandoData(agora + 2000))
        assertFalse(t.copy(em = null).aguardandoData(agora))
    }

    @Test
    fun `os rotulos de tempo e energia`() {
        assertEquals("até 5 min", rotuloTempo(5))
        assertEquals("até 15 min", rotuloTempo(15))
        assertEquals("45 min ou mais", rotuloTempo(45))
        assertEquals("Pouca energia", rotuloEnergia(1))
        assertEquals("Energia média", rotuloEnergia(2))
        assertEquals("Muita energia", rotuloEnergia(3))
        assertEquals("Zumbi", rotuloMeuNivel(1))
        assertEquals("Moderado", rotuloMeuNivel(2))
        assertEquals("Foco", rotuloMeuNivel(3))
    }
}
