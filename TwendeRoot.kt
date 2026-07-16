package co.nedlink.twende.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Twende {
    val Cosmic = Color(0xFF0B0C10)
    val Panel = Color(0xFF12141C)
    val Cyan = Color(0xFF00E5FF)
    val Magenta = Color(0xFFFF007F)
    val Dim = Color(0xFF7C8794)
    val Line = Color(0x26FFFFFF)      // white 15%
    val GlassFill = Color(0x1AFFFFFF) // white 10%
}

private val Scheme = darkColorScheme(
    primary = Twende.Cyan,
    secondary = Twende.Magenta,
    background = Twende.Cosmic,
    surface = Twende.Panel,
    onPrimary = Twende.Cosmic,
    onBackground = Color(0xFFE8ECF1),
    onSurface = Color(0xFFE8ECF1),
    outline = Twende.Line,
)

/** Neon digital style for the clock and gauge numerals. */
fun neonStyle(color: Color, size: Int, glow: Float = 1f) = TextStyle(
    color = color,
    fontSize = size.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    shadow = Shadow(color = color.copy(alpha = 0.85f * glow), offset = Offset.Zero, blurRadius = 22f * glow),
)

@Composable
fun TwendeTheme(content: @Composable () -> Unit) {
    isSystemInDarkTheme() // launcher is always night-cockpit; hook kept for future day mode
    MaterialTheme(colorScheme = Scheme, content = content)
}
