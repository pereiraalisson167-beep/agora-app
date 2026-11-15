package br.com.agora.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Confere a regra da tela "Agora": o que cabe, em que ordem e quantas trocas sobram. */
class EscolhaTest {

    private val agora = 1_700_000_000_000L
    private val umDia = 86_400_000L

    private fun tarefa(
        id: Long, titulo: String, minutos: Int, energia: Int, diasEsperando: Int = 0,
        em: Long? = null, obs: String = ""
    ) = Tarefa(
        id = id, titulo = titulo, minutos = minutos, energia = energia,
        criadaEm = agora - diasEsperando * umDia, em = em, obs = obs
    )

    @Test
    fun `nao cabe tarefa mais longa que o tempo disponivel`() {
        val lista = listOf(tarefa(1, "Longa", 45, 1), tarefa(2, "Curta", 5, 1))
        val fora = cabem(lista, energia = 3, tempo = 5, agora = agora)
        assertEquals(listOf("Curta"), fora.map { it.titulo })
    }

    @Test
    fun `nao cabe tarefa que pede mais energia do que a pessoa tem`() {
        val lista = listOf(tarefa(1, "Pesada", 5, 3), tarefa(2, "Leve", 5, 1))
        val fora = cabem(lista, energia = 1, tempo = 45, agora = agora)
        assertEquals(listOf("Leve"), fora.map { it.titulo })
    }

    @Test
    fun `tarefa com data so entra depois da hora marcada`() {
        val futura = tarefa(1, "Consulta", 15, 2, em = agora + 60_000)
        val vencida = tarefa(2, "Ligar", 15, 2, em = agora - 60_000)
        val fora = cabem(listOf(futura, vencida), energia = 2, tempo = 15, agora = agora)
        assertEquals(listOf("Ligar"), fora.map { it.titulo })
    }

    @Test
    fun `tarefa com hora marcada vem antes das outras`() {
        val lista = listOf(
            tarefa(1, "Esperando muito", 5, 2, diasEsperando = 30),
            tarefa(2, "Marcada", 5, 2, em = agora - 1000)
        )
        val fora = cabem(lista, energia = 2, tempo = 45, agora = agora)
        assertEquals("Marcada", fora.first().titulo)
    }

    @Test
    fun `entre as que cabem vem primeiro a que combina com a energia`() {
        val lista = listOf(tarefa(1, "Pede pouca", 5, 1), tarefa(2, "Pede media", 5, 2))
        val fora = cabem(lista, energia = 2, tempo = 45, agora = agora)
        assertEquals(listOf("Pede media", "Pede pouca"), fora.map { it.titulo })
    }

    @Test
    fun `empatadas vem primeiro a que espera ha mais tempo`() {
        val lista = listOf(
            tarefa(1, "De ontem", 5, 2, diasEsperando = 1),
            tarefa(2, "De duas semanas", 5, 2, diasEsperando = 14)
        )
        val fora = cabem(lista, energia = 2, tempo = 45, agora = agora)
        assertEquals(listOf("De duas semanas", "De ontem"), fora.map { it.titulo })
    }

    @Test
    fun `as trocas respeitam o limite e a quantidade que cabe`() {
        assertEquals(2, trocasRestantes(limite = 2, trocasFeitas = 0, quantasCabem = 5, indice = 0))
        assertEquals(1, trocasRestantes(limite = 2, trocasFeitas = 1, quantasCabem = 5, indice = 1))
        assertEquals(0, trocasRestantes(limite = 2, trocasFeitas = 2, quantasCabem = 5, indice = 2))
        // Com só duas tarefas, uma troca é tudo o que existe.
        assertEquals(1, trocasRestantes(limite = 2, trocasFeitas = 0, quantasCabem = 2, indice = 0))
        assertEquals(0, trocasRestantes(limite = 2, trocasFeitas = 1, quantasCabem = 2, indice = 1))
    }

    @Test
    fun `o recado de troca explica por que nao da para trocar`() {
        assertEquals("Só esta cabe no seu momento.", recadoDeTroca(2, 0, 1, 0))
        assertEquals("", recadoDeTroca(2, 0, 5, 2))
        assertEquals("As trocas acabaram. Vamos com esta!", recadoDeTroca(2, 2, 5, 0))
        assertEquals("Não há outra que caiba agora.", recadoDeTroca(2, 1, 2, 0))
    }

    @Test
    fun `a explicacao muda quando a tarefa tem hora marcada`() {
        val marcada = tarefa(1, "Consulta", 15, 2, em = agora - 1000, obs = "Levar exames")
        val texto = porQueEsta(marcada, energia = 2, agora = agora)
        assertTrue(texto.startsWith("Chegou a hora marcada:"))
        assertTrue(texto.endsWith("Obs.: Levar exames"))
    }

    @Test
    fun `a explicacao conta ha quantos dias a tarefa espera`() {
        assertTrue(porQueEsta(tarefa(1, "Hoje", 5, 2), 2, agora).endsWith("foi anotada hoje."))
        assertTrue(porQueEsta(tarefa(2, "Ontem", 5, 2, 1), 2, agora).endsWith("esperando há 1 dia."))
        assertTrue(porQueEsta(tarefa(3, "Antes", 5, 2, 9), 2, agora).endsWith("esperando há 9 dias."))
        assertTrue(porQueEsta(tarefa(4, "Sobra", 5, 1), 3, agora).startsWith("Pede menos energia"))
    }

    @Test
    fun `o resumo do dia usa singular e plural`() {
        assertEquals("1 tarefa concluída hoje.", resumoDoDia(1))
        assertEquals("2 tarefas concluídas hoje.", resumoDoDia(2))
        assertEquals("0 tarefas concluídas hoje.", resumoDoDia(0))
    }

    @Test
    fun `a proxima com data e a mais perto no futuro`() {
        val lista = listOf(
            tarefa(1, "Depois", 5, 2, em = agora + 5 * umDia),
            tarefa(2, "Logo", 5, 2, em = agora + umDia),
            tarefa(3, "Sem data", 5, 2),
            tarefa(4, "Passada", 5, 2, em = agora - umDia)
        )
        assertEquals("Logo", proximaComData(lista, agora)?.titulo)
    }
}
