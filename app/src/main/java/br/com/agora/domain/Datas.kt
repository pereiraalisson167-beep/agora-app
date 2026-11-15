package br.com.agora.domain

import java.util.Calendar
import java.util.Locale

/**
 * A data da tarefa é escolhida em cinco passos: dia, mês, ano, horário e observação.
 * Cada passo só oferece o que ainda pode dar uma data válida e futura, para a pessoa
 * não conseguir montar uma data que já passou.
 */

private val DIAS_SEMANA = listOf("dom", "seg", "ter", "qua", "qui", "sex", "sáb")

val MESES = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

private fun doisDigitos(n: Int): String = if (n < 10) "0$n" else n.toString()

/** Quantos dias tem o mês [mes] (1 a 12) do ano [ano]. */
fun diasNoMes(mes: Int, ano: Int): Int {
    val c = Calendar.getInstance()
    c.clear()
    c.set(ano, mes - 1, 1)
    return c.getActualMaximum(Calendar.DAY_OF_MONTH)
}

/** Monta um instante em milissegundos a partir das partes escolhidas. */
fun instanteDe(dia: Int, mes: Int, ano: Int, hora: Int, minuto: Int): Long {
    val c = Calendar.getInstance()
    c.clear()
    c.set(ano, mes - 1, dia, hora, minuto)
    return c.timeInMillis
}

/** O mês serve para o dia escolhido? (29 de fevereiro fica para o passo do ano) */
fun mesServe(dia: Int?, mes: Int): Boolean {
    if (dia == null) return true
    val maximo = when (mes) {
        2 -> 29
        4, 6, 9, 11 -> 30
        else -> 31
    }
    return dia <= maximo
}

/** O ano serve? A data precisa existir e o dia não pode já ter acabado. */
fun anoServe(dia: Int?, mes: Int?, ano: Int, agora: Long): Boolean {
    if (dia == null || mes == null) return true
    if (dia > diasNoMes(mes, ano)) return false
    val c = Calendar.getInstance()
    c.clear()
    c.set(ano, mes - 1, dia, 23, 59, 59)
    return c.timeInMillis > agora
}

/** A hora serve? Sobra ao menos um punhado de minutos dentro dela. */
fun horaServe(dia: Int?, mes: Int?, ano: Int?, hora: Int, agora: Long): Boolean {
    if (dia == null || mes == null || ano == null) return true
    return instanteDe(dia, mes, ano, hora, 55) > agora
}

/** O minuto serve? O instante completo precisa estar no futuro. */
fun minutoServe(dia: Int?, mes: Int?, ano: Int?, hora: Int?, minuto: Int, agora: Long): Boolean {
    if (dia == null || mes == null || ano == null || hora == null) return true
    return instanteDe(dia, mes, ano, hora, minuto) > agora
}

/** "ter, 18/11 às 09:30" — o ano só aparece quando não é o ano corrente. */
fun formatarQuando(instante: Long, agora: Long = System.currentTimeMillis()): String {
    val c = Calendar.getInstance()
    c.timeInMillis = instante
    val hoje = Calendar.getInstance()
    hoje.timeInMillis = agora
    val semana = DIAS_SEMANA[c.get(Calendar.DAY_OF_WEEK) - 1]
    val ano = if (c.get(Calendar.YEAR) != hoje.get(Calendar.YEAR)) "/" + c.get(Calendar.YEAR) else ""
    return semana + ", " + doisDigitos(c.get(Calendar.DAY_OF_MONTH)) + "/" +
        doisDigitos(c.get(Calendar.MONTH) + 1) + ano + " às " +
        doisDigitos(c.get(Calendar.HOUR_OF_DAY)) + ":" + doisDigitos(c.get(Calendar.MINUTE))
}

/** "2d 05:12:40" — quanto falta, em dias, horas, minutos e segundos. */
fun formatarFalta(ms: Long): String {
    var s = if (ms <= 0L) 0L else ms / 1000L
    val d = s / 86_400L; s -= d * 86_400L
    val h = s / 3_600L; s -= h * 3_600L
    val m = s / 60L; s -= m * 60L
    val inicio = if (d > 0L) "${d}d " else ""
    return inicio + doisDigitos(h.toInt()) + ":" + doisDigitos(m.toInt()) + ":" + doisDigitos(s.toInt())
}

/** "18/11/2026 · 09:30" — o resumo que acompanha os passos da data. */
fun resumoDaData(dia: Int?, mes: Int?, ano: Int?, hora: Int?, minuto: Int?): String {
    val dd = if (dia != null) doisDigitos(dia) else "--"
    val mm = if (mes != null) doisDigitos(mes) else "--"
    val aa = ano?.toString() ?: "----"
    val hh = if (hora != null) doisDigitos(hora) else "--"
    val mi = if (minuto != null) doisDigitos(minuto) else "--"
    return "$dd/$mm/$aa · $hh:$mi"
}

/** O ano de hoje, usado para oferecer este ano e os dois seguintes. */
fun anoDe(instante: Long): Int {
    val c = Calendar.getInstance(Locale.getDefault())
    c.timeInMillis = instante
    return c.get(Calendar.YEAR)
}
