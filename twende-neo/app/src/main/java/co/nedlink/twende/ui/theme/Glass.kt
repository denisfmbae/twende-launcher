package co.nedlink.twende.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Glassmorphism card. True backdrop blur needs API 31 RenderEffect on a
 * RenderNode behind the card; on 28–30 head units it silently costs frames.
 * We emulate with a 10% white fill + 15% hairline + top sheen — visually
 * equivalent at arm's length, ~free to render.
 */
fun Modifier.glass(radius: Int = 20): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return this
        .clip(shape)
        .background(Twende.GlassFill)
        .border(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x14FFFFFF))), shape)
        // The Settings "glow intensity" slider drives this accent halo on every
        // card and app tile — turn it up and the whole cockpit lights in the
        // chosen colour; down to a whisper for night driving.
        .border(1.5.dp, Twende.Cyan.copy(alpha = (0.06f + 0.38f * Twende.glowLevel).coerceIn(0f, 0.65f)), shape)
}

/** Cosmic backdrop: radial glow pools + a perspective grid, drawn once and cached. */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier.fillMaxSize()) {
    androidx.compose.foundation.Canvas(
        modifier.drawWithCache {
            val grid = Color(0x0E00E5FF)
            onDrawBehind {
                drawRect(Twende.Cosmic)
                if (Twende.isLight) {
                    // Day: a clean bright sheet with a whisper of sky gradient.
                    drawRect(Brush.verticalGradient(
                        listOf(Color(0x1400A0C8), Color.Transparent),
                        endY = size.height * 0.4f))
                    return@onDrawBehind
                }
                drawRect(Brush.radialGradient(
                    listOf(Twende.Cyan.copy(alpha = hiAlpha), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.1f), radius = size.width * 0.5f))
                drawRect(Brush.radialGradient(
                    listOf(Twende.Magenta.copy(alpha = loAlpha), Color.Transparent),
                    center = Offset(size.width * 0.08f, size.height * 0.95f), radius = size.width * 0.45f))
                val horizon = size.height * 0.55f
                var x = -size.width
                while (x < size.width * 2) {
                    drawLine(grid, Offset(size.width / 2, horizon), Offset(x, size.height), 1f)
                    x += size.width / 9
                }
                var y = horizon
                var step = 8f
                while (y < size.height) {
                    drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step; step *= 1.45f
                }
            }
        }
    ) {}
}
