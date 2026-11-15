package br.com.agora.domain

/**
 * A regra da tela "Agora": dada a energia e o tempo que a pessoa tem, quais tarefas
 * cabem e em que ordem.
 *
 * Cabe a tarefa que leva no máximo o tempo disponível, pede no máximo a energia
 * informada e, se tiver data, já chegou a hora marcada.
 *
 * A ordem é:
 *  1. tarefas com hora marcada que já venceu, da mais antiga para a mais nova;
 *  2. as que combinam exatamente com a energia informada;
 *  3. as que pedem mais energia antes das que pedem menos;
 *  4. as que esperam há mais tempo.
 */
fun cabem(tarefas: List<Tarefa>, energia: Int, tempo: Int, agora: Long): List<Tarefa> =
    tarefas
        .filter { it.minutos <= tempo && it.energia <= energia && !it.aguardandoData(agora) }
        .sortedWith(
            compareBy<Tarefa> { if (it.em != null) 0 else 1 }
                .thenBy { it.em ?: Long.MAX_VALUE }
                .thenBy { if (it.energia == energia) 0 else 1 }
                .thenByDescending { it.energia }
                .thenByDescending { it.diasEsperando(agora) }
                .thenBy { it.id }
        )

/**
 * Quantas trocas ainda sobram: nunca mais do que o limite combinado, nem mais do que
 * a quantidade de tarefas que ainda vêm depois da atual.
 */
fun trocasRestantes(limite: Int, trocasFeitas: Int, quantasCabem: Int, indice: Int): Int {
    val porLimite = limite - trocasFeitas
    val porFila = quantasCabem - 1 - indice
    return maxOf(0, minOf(porLimite, porFila))
}

/** O recado embaixo do botão de trocar, ou vazio quando não há nada a dizer. */
fun recadoDeTroca(limite: Int, trocasFeitas: Int, quantasCabem: Int, restantes: Int): String = when {
    quantasCabem == 1 -> "Só esta cabe no seu momento."
    restantes > 0 -> ""
    trocasFeitas >= limite -> "As trocas acabaram. Vamos com esta!"
    else -> "Não há outra que caiba agora."
}

/** A explicação "Por que esta?" que aparece no cartão da tarefa. */
fun porQueEsta(tarefa: Tarefa, energia: Int, agora: Long): String {
    if (tarefa.em != null) {
        val obs = if (tarefa.obs.isBlank()) "" else " Obs.: " + tarefa.obs
        return "Chegou a hora marcada: " + formatarQuando(tarefa.em) + "." + obs
    }
    val encaixe =
        if (tarefa.energia == energia) "Combina com a sua energia"
        else "Pede menos energia do que você tem"
    val dias = tarefa.diasEsperando(agora)
    val espera = when (dias) {
        0 -> " e foi anotada hoje."
        1 -> " e está esperando há 1 dia."
        else -> " e está esperando há $dias dias."
    }
    return encaixe + espera
}

/** A próxima tarefa com data que ainda não chegou, para a contagem da tela inicial. */
fun proximaComData(tarefas: List<Tarefa>, agora: Long): Tarefa? =
    tarefas.filter { it.em != null && it.em > agora }.minByOrNull { it.em!! }

/** "2 tarefas concluídas hoje." — com singular e plural. */
fun resumoDoDia(quantas: Int): String =
    if (quantas == 1) "1 tarefa concluída hoje." else "$quantas tarefas concluídas hoje."
