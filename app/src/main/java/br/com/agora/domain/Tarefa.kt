package br.com.agora.domain

/**
 * Uma tarefa anotada pela pessoa.
 *
 * [minutos] e [energia] são o que a tela "Agora" usa para decidir se a tarefa cabe
 * no momento: quanto tempo ela leva e quanta energia ela pede.
 *
 * @param minutos quanto tempo a tarefa leva: 5, 15 ou 45 (45 significa "45 min ou mais").
 * @param energia quanta energia ela pede: 1 pouca, 2 média, 3 muita.
 * @param criadaEm quando foi anotada, em milissegundos; serve para saber há quanto tempo espera.
 * @param em hora marcada, em milissegundos, ou null quando a tarefa não tem data.
 * @param obs observação, obrigatória para tarefa com data.
 */
data class Tarefa(
    val id: Long = 0L,
    val titulo: String,
    val minutos: Int,
    val energia: Int,
    val criadaEm: Long,
    val em: Long? = null,
    val obs: String = ""
) {
    /** Há quantos dias inteiros a tarefa está esperando. */
    fun diasEsperando(agora: Long): Int {
        val ms = agora - criadaEm
        return if (ms <= 0L) 0 else (ms / 86_400_000L).toInt()
    }

    /** A tarefa com data só vale a partir da hora marcada. */
    fun aguardandoData(agora: Long): Boolean = em != null && em > agora
}

/** Os três tempos que a tela oferece. */
val TEMPOS = listOf(5, 15, 45)

/** Os três níveis de energia que a tela oferece. */
val ENERGIAS = listOf(1, 2, 3)

fun rotuloTempo(minutos: Int): String = when {
    minutos <= 5 -> "até 5 min"
    minutos <= 15 -> "até 15 min"
    else -> "45 min ou mais"
}

fun rotuloEnergia(energia: Int): String = when (energia) {
    1 -> "Pouca energia"
    2 -> "Energia média"
    else -> "Muita energia"
}

fun rotuloEnergiaCurto(energia: Int): String = when (energia) {
    1 -> "Pouca"
    2 -> "Média"
    else -> "Muita"
}

fun rotuloMeuNivel(energia: Int): String = when (energia) {
    1 -> "Zumbi"
    2 -> "Moderado"
    else -> "Foco"
}
