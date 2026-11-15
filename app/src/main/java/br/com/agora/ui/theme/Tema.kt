package br.com.agora.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** As cores vêm do protótipo da tela "Agora": fundo escuro e âmbar como destaque. */
object Cores {
    val Fundo = Color(0xFF15141A)
    val Cartao = Color(0xFF201F27)
    val CartaoAlto = Color(0xFF2B2A33)
    val Borda = Color(0xFF3A3844)
    val Texto = Color(0xFFF3F0EA)
    val TextoFraco = Color(0xFFB4AFA6)
    val TextoFraquinho = Color(0xFF9A958D)
    val Destaque = Color(0xFFF2B35B)
    val DestaqueClaro = Color(0xFFF6C67F)
    val SobreDestaque = Color(0xFF1A1206)
    val Barra = Color(0xFF1C1B22)
}

private val esquema = darkColorScheme(
    primary = Cores.Destaque,
    onPrimary = Cores.SobreDestaque,
    background = Cores.Fundo,
    onBackground = Cores.Texto,
    surface = Cores.Cartao,
    onSurface = Cores.Texto,
    surfaceVariant = Cores.CartaoAlto,
    onSurfaceVariant = Cores.TextoFraco,
    outline = Cores.Borda,
    error = Cores.DestaqueClaro
)

/** O app tem um só visual, escuro, igual ao do protótipo — não segue o tema do sistema. */
@Composable
fun TemaAgora(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, content = content)
}
