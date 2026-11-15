package br.com.agora.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.agora.ui.theme.Cores

/** Os botões e cartões que as três telas repetem. */

/** Botão de escolha: fica claro quando está marcado, como no protótipo. */
@Composable
fun Chip(
    texto: String,
    marcado: Boolean,
    modifier: Modifier = Modifier,
    altura: Int = 52,
    icone: Painter? = null,
    emColuna: Boolean = false,
    ligado: Boolean = true,
    aoTocar: () -> Unit
) {
    val fundo = if (marcado) Cores.Texto else Color.Transparent
    val frente = if (marcado) Cores.Fundo else Cores.Texto
    val borda = if (marcado) Cores.Texto else Cores.Borda
    val opacidade = if (ligado) 1f else 0.35f
    Box(
        modifier = modifier
            .height(altura.dp)
            .background(fundo.copy(alpha = fundo.alpha * opacidade), RoundedCornerShape(14.dp))
            .border(1.5.dp, borda.copy(alpha = opacidade), RoundedCornerShape(14.dp))
            .clickable(enabled = ligado, onClick = aoTocar),
        contentAlignment = Alignment.Center
    ) {
        if (emColuna) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (icone != null) {
                    Icon(painter = icone, contentDescription = null, tint = frente.copy(alpha = opacidade), modifier = Modifier.size(22.dp))
                }
                Text(texto, color = frente.copy(alpha = opacidade), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icone != null) {
                    Icon(painter = icone, contentDescription = null, tint = frente.copy(alpha = opacidade), modifier = Modifier.size(18.dp))
                }
                Text(texto, color = frente.copy(alpha = opacidade), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** O botão cheio, âmbar, das ações principais. */
@Composable
fun BotaoCheio(
    texto: String,
    modifier: Modifier = Modifier,
    icone: Painter? = null,
    ligado: Boolean = true,
    altura: Int = 56,
    aoTocar: () -> Unit
) {
    Button(
        onClick = aoTocar,
        enabled = ligado,
        modifier = modifier.height(altura.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cores.Destaque,
            contentColor = Cores.SobreDestaque,
            disabledContainerColor = Cores.Destaque.copy(alpha = 0.45f),
            disabledContentColor = Cores.SobreDestaque.copy(alpha = 0.6f)
        )
    ) {
        if (icone != null) {
            Icon(painter = icone, contentDescription = null, modifier = Modifier.padding(end = 8.dp).size(22.dp))
        }
        Text(texto, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** O botão vazado, de contorno, das ações secundárias. */
@Composable
fun BotaoVazado(
    texto: String,
    modifier: Modifier = Modifier,
    icone: Painter? = null,
    ligado: Boolean = true,
    altura: Int = 52,
    aoTocar: () -> Unit
) {
    OutlinedButton(
        onClick = aoTocar,
        enabled = ligado,
        modifier = modifier.height(altura.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 10.dp),
        border = BorderStroke(1.5.dp, if (ligado) Cores.Borda else Cores.Borda.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Cores.Texto,
            disabledContentColor = Cores.Texto.copy(alpha = 0.45f)
        )
    ) {
        if (icone != null) {
            Icon(painter = icone, contentDescription = null, modifier = Modifier.padding(end = 6.dp).size(18.dp))
        }
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** O cartão escuro que embrulha o conteúdo de cada tela. */
@Composable
fun Cartao(
    modifier: Modifier = Modifier,
    espaco: Int = 12,
    alinhamento: Alignment.Horizontal = Alignment.Start,
    conteudo: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Cores.Cartao, RoundedCornerShape(22.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(espaco.dp),
        horizontalAlignment = alinhamento,
        content = conteudo
    )
}

/** Marca de etiqueta: "SUA TAREFA AGORA". */
@Composable
fun Etiqueta(texto: String) {
    Text(
        texto.uppercase(),
        color = Cores.Destaque,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.4.sp
    )
}
