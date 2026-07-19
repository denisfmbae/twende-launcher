package co.nedlink.twende.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * One switch, two cockpits. Every colour is a getter over [isLight], so the
 * whole app re-skins live when the driver taps DAY/NIGHT — no call site
 * changed, no restart. Day mode exists because a dark launcher is unreadable
 * in equatorial sun; night mode because day colours would blind at 9 pm.
 */
object Twende {
    var isLight by mutableStateOf(false)

    val Cosmic get() = if (isLight) Color(0xFFF2F5F9) else Color(0xFF0B0C10)
    val Panel get() = if (isLight) Color(0xFFFFFFFF) else Color(0xFF12141C)
    val Cyan get() = if (isLight) Color(0xFF006E96) else Color(0xFF00E5FF)
    val Magenta get() = if (isLight) Color(0xFFC2185B) else Color(0xFFFF007F)
    val Dim get() = if (isLight) Color(0xFF55606B) else Color(0xFF7C8794)
    val Line get() = if (isLight) Color(0x2E000000) else Color(0x26FFFFFF)
    val GlassFill get() = if (isLight) Color(0xCCFFFFFF) else Color(0x1AFFFFFF)
    /** Primary text: near-black by day, near-white by night. */
    val Ink get() = if (isLight) Color(0xFF141A21) else Color(0xFFE8ECF1)
    /** Neutral button backdrop that reads on both backgrounds. */
    val ButtonBg get() = if (isLight) Color(0x14000000) else Color(0x1AFFFFFF)
}

/** Neon digital style; the glow bloom is toned down by day so text stays crisp. */
fun neonStyle(color: Color, size: Int, glow: Float = 1f): TextStyle {
    val bloom = if (Twende.isLight) 6f else 22f
    return TextStyle(
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        shadow = Shadow(color = color.copy(alpha = 0.85f * glow), offset = Offset.Zero, blurRadius = bloom * glow),
    )
}

@Composable
fun TwendeTheme(content: @Composable () -> Unit) {
    val scheme = if (Twende.isLight) lightColorScheme(
        primary = Twende.Cyan, secondary = Twende.Magenta,
        background = Twende.Cosmic, surface = Twende.Panel,
        onPrimary = Color.White, onBackground = Twende.Ink,
        onSurface = Twende.Ink, outline = Twende.Line,
    ) else darkColorScheme(
        primary = Twende.Cyan, secondary = Twende.Magenta,
        background = Twende.Cosmic, surface = Twende.Panel,
        onPrimary = Twende.Cosmic, onBackground = Twende.Ink,
        onSurface = Twende.Ink, outline = Twende.Line,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
