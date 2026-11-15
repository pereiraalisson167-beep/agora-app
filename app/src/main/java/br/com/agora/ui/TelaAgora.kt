package br.com.agora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.agora.R
import br.com.agora.domain.ENERGIAS
import br.com.agora.domain.TEMPOS
import br.com.agora.domain.formatarFalta
import br.com.agora.domain.formatarQuando
import br.com.agora.domain.porQueEsta
import br.com.agora.domain.proximaComData
import br.com.agora.domain.recadoDeTroca
import br.com.agora.domain.resumoDoDia
import br.com.agora.domain.rotuloEnergia
import br.com.agora.domain.rotuloMeuNivel
import br.com.agora.domain.rotuloTempo
import br.com.agora.ui.hourglass.Ampulheta
import br.com.agora.ui.theme.Cores

/** A tela única do app: escolher, fazer e comemorar. */
@Composable
fun TelaAgora(estado: EstadoTela, acoes: Acoes) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Cores.Fundo)
            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 22.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Cabecalho(estado)

            Box(Modifier.weight(1f)) {
                when (estado.modo) {
                    Modo.ESCOLHER -> BlocoEscolher(estado, acoes)
                    Modo.FAZENDO -> BlocoFazendo(estado, acoes)
                    Modo.FEITO -> BlocoFeito(estado, acoes)
                }
            }

            BotaoVazado(
                texto = "Anotar tarefa",
                icone = painterResource(R.drawable.ic_mais),
                altura = 56,
                modifier = Modifier.fillMaxWidth()
            ) { acoes.abrirFolha() }
        }

        // O aviso fica no alto para não cobrir os botões de ação.
        AnimatedVisibility(
            visible = estado.aviso.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            Aviso(estado.aviso)
        }
        LaunchedEffect(estado.aviso) {
            if (estado.aviso.isNotEmpty()) {
                kotlinx.coroutines.delay(2600)
                acoes.limparAviso()
            }
        }

        if (estado.passo != Passo.FECHADA) AnotarFolha(estado, acoes)
    }
}

@Composable
private fun Aviso(texto: String) {
    Text(
        texto,
        modifier = Modifier
            .fillMaxWidth()
            .background(Cores.Texto, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        color = Cores.Fundo,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Cabecalho(estado: EstadoTela) {
    val proxima = remember(estado.tarefas, estado.relogio, estado.modo) {
        if (estado.modo == Modo.ESCOLHER) proximaComData(estado.tarefas, estado.relogio) else null
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "Agora", color = Cores.Texto, fontSize = 34.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val feitas = estado.concluidasHoje
            Row(
                Modifier
                    .background(
                        if (feitas > 0) Cores.Destaque.copy(alpha = 0.16f) else Cores.Cartao,
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = if (feitas > 0) Cores.DestaqueClaro else Cores.TextoFraco,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Hoje: $feitas",
                    color = if (feitas > 0) Cores.DestaqueClaro else Cores.TextoFraco,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (proxima == null) {
            Text(
                "Diga como você está. Eu escolho por você.",
                color = Cores.TextoFraco, fontSize = 15.sp
            )
        }

        if (proxima != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Cores.Cartao, RoundedCornerShape(16.dp))
                    .border(1.5.dp, Cores.Borda, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.ic_ampulheta),
                    contentDescription = "Próxima tarefa com data",
                    tint = Cores.Destaque,
                    modifier = Modifier.size(22.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        proxima.titulo, color = Cores.Texto, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatarQuando(proxima.em!!, estado.relogio),
                        color = Cores.TextoFraco, fontSize = 12.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("faltam", color = Cores.TextoFraquinho, fontSize = 11.sp)
                    Text(
                        formatarFalta(proxima.em!! - estado.relogio),
                        color = Cores.DestaqueClaro, fontSize = 19.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- escolher
@Composable
private fun BlocoEscolher(estado: EstadoTela, acoes: Acoes) {
    val candidatas = estado.candidatas()
    val escolhida = estado.escolhida
    val sobrando = estado.trocasSobrando(LIMITE_DE_TROCAS)

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Como está sua energia?", color = Cores.Texto, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ENERGIAS.forEach { nivel ->
                    Chip(
                        texto = rotuloMeuNivel(nivel),
                        marcado = estado.energia == nivel,
                        modifier = Modifier.weight(1f),
                        altura = 76,
                        icone = painterResource(iconeDaEnergia(nivel)),
                        emColuna = true
                    ) { acoes.escolherEnergia(nivel) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Quanto tempo você tem?", color = Cores.Texto, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TEMPOS.forEach { minutos ->
                    Chip(
                        texto = if (minutos >= 45) "45 min+" else "$minutos min",
                        marcado = estado.tempo == minutos,
                        modifier = Modifier.weight(1f)
                    ) { acoes.escolherTempo(minutos) }
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when {
                estado.energia == null || estado.tempo == null -> Convite(estado)
                candidatas.isEmpty() -> NadaCabe(acoes)
                escolhida != null -> CartaoDaTarefa(estado, escolhida, candidatas.size, sobrando, acoes)
            }
        }
    }
}

@Composable
private fun Convite(estado: EstadoTela) {
    val recado = when {
        estado.energia != null && estado.tempo == null -> "Agora, quanto tempo você tem?"
        estado.energia == null && estado.tempo != null -> "Agora, como está a sua energia?"
        else -> "Toque nas duas escolhas acima. Eu mostro uma tarefa só."
    }
    Column(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val traco = 1.5.dp.toPx()
                drawRoundRect(
                    color = Cores.Borda,
                    topLeft = Offset(traco / 2, traco / 2),
                    size = Size(size.width - traco, size.height - traco),
                    cornerRadius = CornerRadius(22.dp.toPx()),
                    style = Stroke(
                        width = traco,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                    )
                )
            }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painterResource(R.drawable.ic_subir),
            contentDescription = null,
            tint = Cores.TextoFraco,
            modifier = Modifier.size(44.dp)
        )
        Text(
            recado, color = Cores.TextoFraco, fontSize = 16.sp,
            textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp)
        )
    }
}

@Composable
private fun NadaCabe(acoes: Acoes) {
    Cartao(Modifier.fillMaxSize(), espaco = 14, alinhamento = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Icon(
            painterResource(R.drawable.ic_lua),
            contentDescription = null,
            tint = Cores.Destaque,
            modifier = Modifier.size(44.dp)
        )
        Text(
            "Nada cabe agora.\nE tudo bem.",
            color = Cores.Texto, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, lineHeight = 30.sp
        )
        Text(
            "Que tal uma pausa curta? Ou anote o que está na sua cabeça.",
            color = Cores.TextoFraco, fontSize = 15.sp, textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        Spacer(Modifier.weight(1f))
        BotaoCheio(
            texto = "Pausa de 2 min",
            icone = painterResource(R.drawable.ic_pausa),
            altura = 52,
            modifier = Modifier.fillMaxWidth()
        ) { acoes.comecarPausa() }
        BotaoVazado(texto = "Anotar", modifier = Modifier.fillMaxWidth()) { acoes.abrirFolha() }
    }
}

@Composable
private fun CartaoDaTarefa(
    estado: EstadoTela,
    tarefa: br.com.agora.domain.Tarefa,
    quantasCabem: Int,
    sobrando: Int,
    acoes: Acoes
) {
    val recado = recadoDeTroca(LIMITE_DE_TROCAS, estado.trocasFeitas, quantasCabem, sobrando)
    Cartao(Modifier.fillMaxSize()) {
        Etiqueta("Sua tarefa agora")
        Text(
            tarefa.titulo, color = Cores.Texto, fontSize = 27.sp,
            fontWeight = FontWeight.SemiBold, lineHeight = 31.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Selo(painterResource(R.drawable.ic_relogio), rotuloTempo(tarefa.minutos))
            Selo(painterResource(iconeDaEnergia(tarefa.energia)), rotuloEnergia(tarefa.energia))
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Cores.Texto, fontWeight = FontWeight.SemiBold)) {
                    append("Por que esta? ")
                }
                append(porQueEsta(tarefa, estado.energia ?: tarefa.energia, estado.relogio))
            },
            color = Cores.TextoFraco, fontSize = 15.sp, lineHeight = 22.sp
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BotaoCheio(
                texto = "Começar",
                icone = painterResource(R.drawable.ic_play),
                modifier = Modifier.weight(1.25f)
            ) { acoes.comecar() }
            BotaoVazado(
                texto = if (sobrando > 0) "Trocar ($sobrando)" else "Sem trocas",
                icone = painterResource(R.drawable.ic_trocar),
                ligado = sobrando > 0,
                altura = 56,
                modifier = Modifier.weight(1f)
            ) { acoes.trocar() }
        }
        if (recado.isNotEmpty()) {
            Text(
                recado, color = Cores.TextoFraquinho, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Selo(icone: androidx.compose.ui.graphics.painter.Painter, texto: String) {
    Row(
        Modifier
            .background(Cores.CartaoAlto, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icone, contentDescription = null, tint = Cores.Texto, modifier = Modifier.size(16.dp))
        Text(texto, color = Cores.Texto, fontSize = 14.sp)
    }
}

private fun iconeDaEnergia(energia: Int): Int = when (energia) {
    1 -> R.drawable.ic_energia1
    2 -> R.drawable.ic_energia2
    else -> R.drawable.ic_energia3
}

// ---------------------------------------------------------------- fazendo
@Composable
private fun BlocoFazendo(estado: EstadoTela, acoes: Acoes) {
    val fazendo = estado.fazendo ?: return
    var ampulheta by remember { mutableStateOf<Ampulheta?>(null) }

    Cartao(Modifier.fillMaxSize(), espaco = 16, alinhamento = Alignment.CenterHorizontally) {
        Etiqueta(
            when {
                estado.tempoAcabou -> "O tempo acabou"
                fazendo.ehPausa -> "Pausa"
                else -> "Fazendo agora"
            }
        )
        Text(
            fazendo.titulo, color = Cores.Texto, fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, lineHeight = 29.sp
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(280.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { contexto ->
                    Ampulheta(contexto).also { vista ->
                        vista.contentDescription = "Ampulheta: a areia cai durante o tempo da tarefa"
                        vista.aoFicarPronta = { pronta -> acoes.ampulhetaPronta(pronta) }
                        vista.definirCiclo(fazendo.inicioUptime, fazendo.totalSegundos)
                        ampulheta = vista
                    }
                },
                update = { vista -> vista.definirCiclo(fazendo.inicioUptime, fazendo.totalSegundos) },
                modifier = Modifier.fillMaxSize()
            )
            if (!estado.ampulhetaPronta) {
                Text(
                    "Preparando a ampulheta…",
                    color = Cores.TextoFraco, fontSize = 14.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Spacer(Modifier.weight(1f))
        BotaoCheio(
            texto = "Concluí",
            icone = painterResource(R.drawable.ic_check),
            modifier = Modifier.fillMaxWidth()
        ) { acoes.concluir() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (estado.tempoAcabou) {
                BotaoVazado(texto = "Mais 5 min", modifier = Modifier.weight(1f)) {
                    val espera = ampulheta?.virar() ?: 0L
                    acoes.maisCincoMinutos(espera)
                }
            }
            BotaoVazado(
                texto = "Parar",
                icone = painterResource(R.drawable.ic_parar),
                modifier = Modifier.weight(1f)
            ) { acoes.parar() }
        }
        Text(
            when {
                estado.tempoAcabou -> "Terminou? Se ainda não, peça mais 5 min."
                fazendo.ehPausa -> "Respire fundo e solte os ombros."
                else -> "Parar não apaga nada. A tarefa volta para a lista."
            },
            color = Cores.TextoFraquinho, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------- feito
@Composable
private fun BlocoFeito(estado: EstadoTela, acoes: Acoes) {
    Cartao(Modifier.fillMaxSize(), espaco = 14, alinhamento = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(88.dp)
                .background(Cores.Destaque, RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Cores.SobreDestaque,
                modifier = Modifier.size(44.dp)
            )
        }
        Text("Feito!", color = Cores.Texto, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            estado.ultimaFeita, color = Cores.TextoFraco, fontSize = 16.sp,
            textAlign = TextAlign.Center, textDecoration = TextDecoration.LineThrough
        )
        Text(
            resumoDoDia(estado.concluidasHoje), color = Cores.Texto,
            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.weight(1f))
        BotaoCheio(texto = "Escolher a próxima", modifier = Modifier.fillMaxWidth()) { acoes.proxima() }
    }
}

/** Os toques da tela, reunidos para a tela não conhecer o ViewModel. */
class Acoes(
    val escolherEnergia: (Int) -> Unit,
    val escolherTempo: (Int) -> Unit,
    val trocar: () -> Unit,
    val comecar: () -> Unit,
    val comecarPausa: () -> Unit,
    val concluir: () -> Unit,
    val parar: () -> Unit,
    val proxima: () -> Unit,
    val maisCincoMinutos: (Long) -> Unit,
    val ampulhetaPronta: (Boolean) -> Unit,
    val limparAviso: () -> Unit,
    val abrirFolha: () -> Unit,
    val fecharFolha: () -> Unit,
    val mudarTitulo: (String) -> Unit,
    val mudarMinutos: (Int) -> Unit,
    val mudarEnergia: (Int) -> Unit,
    val mudarObs: (String) -> Unit,
    val pedirData: () -> Unit,
    val responderSim: () -> Unit,
    val responderNao: () -> Unit,
    val voltarPasso: () -> Unit,
    val escolherDia: (Int) -> Unit,
    val escolherMes: (Int) -> Unit,
    val escolherAno: (Int) -> Unit,
    val escolherHora: (Int) -> Unit,
    val escolherMinuto: (Int) -> Unit,
    val horarioPronto: () -> Unit,
    val guardarComData: () -> Unit
)
