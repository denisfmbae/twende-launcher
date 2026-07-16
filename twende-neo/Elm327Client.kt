package co.nedlink.twende.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.roundToInt

val FuelGreen = Color(0xFF00E676)
val FuelYellow = Color(0xFFFFD600)
val FuelOrange = Color(0xFFFF9100)
val FuelRed = Color(0xFFFF1744)

/** Colour bands, descending: green -> yellow -> orange -> red. */
fun fuelColor(pct: Int): Color = when {
    pct >= 60 -> FuelGreen
    pct >= 35 -> FuelYellow
    pct >= 15 -> FuelOrange
    else -> FuelRed
}

fun fuelWord(pct: Int): String = when {
    pct >= 60 -> "GOOD"
    pct >= 35 -> "OK"
    pct >= 15 -> "LOW"
    else -> "RESERVE"
}

/**
 * Segmented fuel gauge. The whole bar takes the colour of the current band, so a
 * glance tells you the state without reading a number. Below the reserve line the
 * lit segments breathe, which catches the eye in peripheral vision while driving.
 */
@Composable
fun FuelBar(
    fuelPct: Int,
    modifier: Modifier = Modifier,
    segments: Int = 14,
    barHeight: Int = 24,
) {
    val pct = fuelPct.coerceIn(0, 100)
    val color = fuelColor(pct)
    val reserve = pct < 15

    val pulse by rememberInfiniteTransition(label = "fuel").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "fuelPulse",
    )
    val alpha = if (reserve) pulse else 1f

    Canvas(modifier.fillMaxWidth().height(barHeight.dp)) {
        val gap = 3f
        val segW = ((size.width - gap * (segments - 1)) / segments).coerceAtLeast(1f)
        val lit = (pct / 100f * segments).roundToInt()
        for (i in 0 until segments) {
            val on = i < lit
            drawRoundRect(
                color = if (on) color.copy(alpha = alpha) else Twende.Line,
                topLeft = Offset(i * (segW + gap), 0f),
                size = Size(segW, size.height),
                cornerRadius = CornerRadius(3f),
            )
        }
    }
}
