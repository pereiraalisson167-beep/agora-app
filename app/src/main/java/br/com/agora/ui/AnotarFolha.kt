package br.com.agora.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.agora.R
import br.com.agora.domain.ENERGIAS
import br.com.agora.domain.MESES
import br.com.agora.domain.TEMPOS
import br.com.agora.domain.anoDe
import br.com.agora.domain.anoServe
import br.com.agora.domain.horaServe
import br.com.agora.domain.mesServe
import br.com.agora.domain.minutoServe
import br.com.agora.domain.resumoDaData
import br.com.agora.domain.rotuloEnergiaCurto
import br.com.agora.ui.theme.Cores

/** A folha que sobe de baixo para anotar uma tarefa, com ou sem data marcada. */
@Composable
fun AnotarFolha(estado: EstadoTela, acoes: Acoes) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xA80A090E))
            .clickable(onClick = acoes.fecharFolha),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Cores.Cartao, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clickable(enabled = false) {}
                .padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 26.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (estado.passo) {
                Passo.FORMULARIO -> Formulario(estado, acoes)
                Passo.PERGUNTA -> Pergunta(acoes)
                else -> PassosDaData(estado, acoes)
            }
        }
    }
}

@Composable
private fun Topo(titulo: String, acoes: Acoes, comVoltar: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (comVoltar) {
            BotaoRedondo(R.drawable.ic_voltar, "Voltar", acoes.voltarPasso)
        }
        Text(
            titulo, color = Cores.Texto, fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)
        )
        BotaoRedondo(R.drawable.ic_fechar, "Fechar", acoes.fecharFolha)
    }
}

@Composable
private fun BotaoRedondo(icone: Int, descricao: String, aoTocar: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .background(Cores.CartaoAlto, RoundedCornerShape(999.dp))
            .clickable(onClick = aoTocar),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icone),
            contentDescription = descricao,
            tint = Cores.Texto,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------------------------------------------------------------- passo 1: o que e quanto
@Composable
private fun Formulario(estado: EstadoTela, acoes: Acoes) {
    val r = estado.rascunho
    Topo("Anotar tarefa", acoes, comVoltar = false)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("O que precisa ser feito?", color = Cores.Texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = r.titulo,
            onValueChange = acoes.mudarTitulo,
            placeholder = { Text("Ex.: Trocar a lâmpada da cozinha", color = Cores.TextoFraquinho) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Cores.Texto,
                unfocusedTextColor = Cores.Texto,
                focusedContainerColor = Cores.Fundo,
                unfocusedContainerColor = Cores.Fundo,
                focusedBorderColor = Cores.Destaque,
                unfocusedBorderColor = Cores.Borda,
                cursorColor = Cores.Destaque
            )
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quanto tempo leva?", color = Cores.Texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TEMPOS.forEach { minutos ->
                Chip(
                    texto = if (minutos >= 45) "45 min+" else "até $minutos min",
                    marcado = r.minutos == minutos,
                    altura = 48,
                    modifier = Modifier.weight(1f)
                ) { acoes.mudarMinutos(minutos) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Quanta energia pede?", color = Cores.Texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ENERGIAS.forEach { nivel ->
                Chip(
                    texto = rotuloEnergiaCurto(nivel),
                    marcado = r.energia == nivel,
                    icone = painterResource(
                        when (nivel) {
                            1 -> R.drawable.ic_energia1
                            2 -> R.drawable.ic_energia2
                            else -> R.drawable.ic_energia3
                        }
                    ),
                    modifier = Modifier.weight(1f)
                ) { acoes.mudarEnergia(nivel) }
            }
        }
    }

    Text(
        "Já vem marcado o mais comum. Ajuste só se precisar.",
        color = Cores.TextoFraquinho, fontSize = 13.sp
    )

    BotaoCheio(
        texto = "Guardar",
        ligado = r.titulo.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
    ) { acoes.pedirData() }
}

// ---------------------------------------------------------------- passo 2: quer data?
@Composable
private fun Pergunta(acoes: Acoes) {
    Topo("Definir data?", acoes, comVoltar = true)
    Text(
        "Com data, a tarefa só entra nas sugestões a partir do dia e da hora marcados. " +
            "Até lá, a tela inicial mostra quanto tempo falta e o lembrete avisa na hora.",
        color = Cores.TextoFraco, fontSize = 15.sp, lineHeight = 22.sp
    )
    BotaoCheio(texto = "Sim, definir data", modifier = Modifier.fillMaxWidth()) { acoes.responderSim() }
    BotaoVazado(texto = "Não, guardar assim", modifier = Modifier.fillMaxWidth()) { acoes.responderNao() }
}

// ---------------------------------------------------------------- passos 3 a 7: a data
@Composable
private fun PassosDaData(estado: EstadoTela, acoes: Acoes) {
    val r = estado.rascunho
    val agora = estado.relogio
    val ordem = listOf(Passo.DIA, Passo.MES, Passo.ANO, Passo.HORARIO, Passo.OBSERVACAO)
    val indice = ordem.indexOf(estado.passo)
    val titulo = when (estado.passo) {
        Passo.DIA -> "Que dia ela começa?"
        Passo.MES -> "Em que mês?"
        Passo.ANO -> "Em que ano?"
        Passo.HORARIO -> "Que horas ela começa?"
        else -> "Escreva uma observação"
    }

    Topo(titulo, acoes, comVoltar = true)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                "Passo ${indice + 1} de 5", color = Cores.Destaque,
                fontSize = 13.sp, fontWeight = FontWeight.Bold
            )
            Text(
                resumoDaData(r.dia, r.mes, r.ano, r.hora, r.minuto),
                color = Cores.TextoFraco, fontSize = 13.sp
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ordem.forEachIndexed { i, _ ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (i <= indice) Cores.Destaque else Cores.Borda,
                            RoundedCornerShape(999.dp)
                        )
                )
            }
        }
    }

    when (estado.passo) {
        Passo.DIA -> Grade((1..31).toList(), 7) { dia ->
            Chip(
                texto = dia.toString(), marcado = r.dia == dia, altura = 44,
                modifier = Modifier.fillMaxWidth()
            ) { acoes.escolherDia(dia) }
        }

        Passo.MES -> Grade(MESES.indices.toList(), 3) { i ->
            val mes = i + 1
            Chip(
                texto = MESES[i], marcado = r.mes == mes, altura = 48,
                ligado = mesServe(r.dia, mes), modifier = Modifier.fillMaxWidth()
            ) { acoes.escolherMes(mes) }
        }

        Passo.ANO -> {
            val base = anoDe(agora)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(base, base + 1, base + 2).forEach { ano ->
                    val serve = anoServe(r.dia, r.mes, ano, agora)
                    Chip(
                        texto = ano.toString(), marcado = r.ano == ano, altura = 56,
                        ligado = serve, modifier = Modifier.fillMaxWidth()
                    ) { acoes.escolherAno(ano) }
                }
            }
        }

        Passo.HORARIO -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hora", color = Cores.Texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Grade((0..23).toList(), 6) { hora ->
                    Chip(
                        texto = doisDigitos(hora), marcado = r.hora == hora, altura = 44,
                        ligado = horaServe(r.dia, r.mes, r.ano, hora, agora),
                        modifier = Modifier.fillMaxWidth()
                    ) { acoes.escolherHora(hora) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Minutos", color = Cores.Texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Grade((0 until 60 step 5).toList(), 6) { minuto ->
                    Chip(
                        texto = doisDigitos(minuto), marcado = r.minuto == minuto, altura = 44,
                        ligado = r.hora == null || minutoServe(r.dia, r.mes, r.ano, r.hora, minuto, agora),
                        modifier = Modifier.fillMaxWidth()
                    ) { acoes.escolherMinuto(minuto) }
                }
            }
            if (r.erro.isNotEmpty()) {
                Text(r.erro, color = Cores.DestaqueClaro, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            val pronto = r.hora != null && r.minuto != null &&
                minutoServe(r.dia, r.mes, r.ano, r.hora, r.minuto!!, agora)
            BotaoCheio(texto = "Continuar", ligado = pronto, modifier = Modifier.fillMaxWidth()) {
                acoes.horarioPronto()
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Observação (obrigatória)", color = Cores.Texto,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = r.obs,
                    onValueChange = acoes.mudarObs,
                    placeholder = { Text("Ex.: Levar o contrato assinado", color = Cores.TextoFraquinho) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 112.dp),
                    minLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Cores.Texto,
                        unfocusedTextColor = Cores.Texto,
                        focusedContainerColor = Cores.Fundo,
                        unfocusedContainerColor = Cores.Fundo,
                        focusedBorderColor = Cores.Destaque,
                        unfocusedBorderColor = Cores.Borda,
                        cursorColor = Cores.Destaque
                    )
                )
            }
            BotaoCheio(
                texto = "Guardar tarefa",
                ligado = r.obs.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { acoes.guardarComData() }
        }
    }
}

/**
 * Uma grade simples, montada linha por linha. Não é uma lista que rola, de propósito:
 * ela fica dentro de uma folha que já rola, e rolagem dentro de rolagem quebra o toque.
 */
@Composable
private fun <T> Grade(itens: List<T>, colunas: Int, celula: @Composable (T) -> Unit) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itens.chunked(colunas).forEach { linha ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                linha.forEach { item ->
                    Box(Modifier.weight(1f)) { celula(item) }
                }
                // Mantém as células do mesmo tamanho quando a última linha não enche.
                repeat(colunas - linha.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    }
}

private fun doisDigitos(n: Int): String = if (n < 10) "0$n" else n.toString()
