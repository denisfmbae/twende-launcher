package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.roundToInt
import kotlin.math.sin

private val CARDS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private fun cardinal(deg: Float) = CARDS[(((deg + 22.5f) / 45f).toInt()) % 8]

private val GLASS_HI = Color(0xFF9FE8FF)
private val HALO = Color(0xFF39B9FF)

/**
 * Centre stage: a clean line-art car, rear view, drawn with smooth curves and a
 * neon rim — the Tesla-cluster look rather than filled programmer-art, which is
 * what the previous version kept collapsing into. Pure outline + glow reads as
 * "a car" instantly at dashboard distance and matches the app's cyberpunk theme.
 * It sits on a breathing halo disc over a floor grid that scrolls with real
 * speed. No door state is drawn here anymore: this hardware exposes no physical
 * door signal, and the widget must not imply information it cannot have.
 */
@Composable
fun CarSimulationWidget(
    speedKmh: Int,
    heading: Float,
    glow: Float,
    modifier: Modifier = Modifier,
) {
    val speed = rememberUpdatedState(speedKmh)
    var phase by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L) {
                    val dt = (t - last) / 1_000_000_000f
                    phase += speed.value * dt * 0.9f
                    if (phase > 100_000f) phase -= 100_000f
                    pulse += dt
                    if (pulse > 1000f) pulse -= 1000f
                }
                last = t
            }
        }
    }

    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawFloor(phase, glow)
            drawHalo(glow, pulse)
            drawOutlineCar(speed.value, glow, pulse)
        }

        Text(
            "${cardinal(heading)}  ${heading.roundToInt()}°",
            color = Twende.Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Text(
            "$speedKmh",
            color = Twende.Cyan, fontSize = 44.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 6.dp),
        )
        Text(
            "km/h",
            color = Twende.Dim, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 84.dp, bottom = 15.dp),
        )
    }
}

private fun DrawScope.drawFloor(phase: Float, glow: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.30f
    val vpx = w / 2f
    val baseA = 0.13f + 0.11f * glow.coerceIn(0f, 1f)
    val cyan = Twende.Cyan.copy(alpha = baseA)

    for (side in listOf(-1f, 1f)) {
        drawLine(cyan, Offset(vpx, horizon), Offset(vpx + side * w * 0.62f, h), strokeWidth = 2f)
        drawLine(cyan.copy(alpha = baseA * 0.6f), Offset(vpx, horizon), Offset(vpx + side * w * 0.46f, h), strokeWidth = 1.5f)
    }
    val bands = 10
    for (i in 0..bands) {
        val p = ((i + (phase % 1f)) / bands).coerceIn(0f, 1f)
        val y = horizon + (h - horizon) * (p * p)
        drawLine(Twende.Cyan.copy(alpha = baseA * (0.25f + 0.6f * p)), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawHalo(glow: Float, pulse: Float) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val cy = h * 0.80f
    val breathe = 0.85f + 0.15f * sin(pulse * 2.2f)
    val g = glow.coerceIn(0f, 1f)

    drawOval(
        Brush.radialGradient(
            listOf(HALO.copy(alpha = 0.26f * breathe * (0.6f + 0.4f * g)), Color.Transparent),
            center = Offset(cx, cy), radius = w * 0.42f,
        ),
        topLeft = Offset(cx - w * 0.44f, cy - h * 0.13f),
        size = Size(w * 0.88f, h * 0.26f),
    )
    val ring = Path().apply {
        addOval(Rect(Offset(cx - w * 0.35f, cy - h * 0.065f), Size(w * 0.70f, h * 0.145f)))
    }
    drawPath(ring, Twende.Cyan.copy(alpha = 0.50f * breathe), style = Stroke(width = 2.5f))
    drawPath(ring, GLASS_HI.copy(alpha = 0.18f), style = Stroke(width = 7f))
}

/**
 * Line-art rear view. One smooth silhouette drawn twice (wide faint stroke for
 * glow, thin bright stroke on top), wheel arches, a beltline, glass, a full-width
 * light bar and a plate. Curves via cubic Béziers so nothing reads as a slab.
 */
private fun DrawScope.drawOutlineCar(speedKmh: Int, glow: Float, pulse: Float) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val moving = speedKmh > 1
    val g = glow.coerceIn(0f, 1f)
    val breathe = 0.9f + 0.1f * sin(pulse * 2.2f)

    val bright = Twende.Cyan.copy(alpha = (0.85f + 0.15f * g) * breathe)
    val glowStroke = Twende.Cyan.copy(alpha = 0.14f * breathe)

    // Key vertical stations
    val groundY = h * 0.80f
    val bumperY = h * 0.735f       // bottom edge of body
    val beltY   = h * 0.545f       // beltline (top of body mass)
    val roofY   = h * 0.335f       // roof crown

    // Key half-widths
    val bodyHalf  = w * 0.295f     // widest point (over rear arches)
    val baseHalf  = w * 0.255f     // bumper bottom corners (tucks in)
    val beltHalf  = w * 0.270f     // shoulders at beltline
    val cabinHalf = w * 0.185f     // greenhouse base
    val roofHalf  = w * 0.120f     // roof width

    // ---------- silhouette (closed, curvy) ----------
    val body = Path().apply {
        moveTo(cx - baseHalf, bumperY)
        // left lower corner → out to arch bulge
        cubicTo(cx - bodyHalf * 0.99f, bumperY - h * 0.005f,
                cx - bodyHalf, bumperY - h * 0.055f,
                cx - bodyHalf, h * 0.655f)
        // left flank up to shoulder
        cubicTo(cx - bodyHalf, h * 0.60f,
                cx - beltHalf * 1.01f, beltY + h * 0.02f,
                cx - beltHalf * 0.96f, beltY)
        // shoulder in to C-pillar base
        cubicTo(cx - beltHalf * 0.80f, beltY - h * 0.012f,
                cx - cabinHalf * 1.12f, beltY - h * 0.02f,
                cx - cabinHalf, beltY - h * 0.028f)
        // C-pillar sweep to roof
        cubicTo(cx - cabinHalf * 0.85f, h * 0.46f,
                cx - roofHalf * 1.35f, roofY + h * 0.02f,
                cx - roofHalf, roofY)
        // roof crown
        cubicTo(cx - roofHalf * 0.45f, roofY - h * 0.018f,
                cx + roofHalf * 0.45f, roofY - h * 0.018f,
                cx + roofHalf, roofY)
        // mirror right side down
        cubicTo(cx + roofHalf * 1.35f, roofY + h * 0.02f,
                cx + cabinHalf * 0.85f, h * 0.46f,
                cx + cabinHalf, beltY - h * 0.028f)
        cubicTo(cx + cabinHalf * 1.12f, beltY - h * 0.02f,
                cx + beltHalf * 0.80f, beltY - h * 0.012f,
                cx + beltHalf * 0.96f, beltY)
        cubicTo(cx + beltHalf * 1.01f, beltY + h * 0.02f,
                cx + bodyHalf, h * 0.60f,
                cx + bodyHalf, h * 0.655f)
        cubicTo(cx + bodyHalf, bumperY - h * 0.055f,
                cx + bodyHalf * 0.99f, bumperY - h * 0.005f,
                cx + baseHalf, bumperY)
        // bumper bottom, slightly curved
        cubicTo(cx + baseHalf * 0.5f, bumperY + h * 0.012f,
                cx - baseHalf * 0.5f, bumperY + h * 0.012f,
                cx - baseHalf, bumperY)
        close()
    }
    drawPath(body, glowStroke, style = Stroke(width = 10f, cap = StrokeCap.Round))
    drawPath(body, bright, style = Stroke(width = 3f, cap = StrokeCap.Round))

    // very light interior wash so the car separates from the grid
    drawPath(body, Brush.verticalGradient(
        listOf(Color(0x1A2A4FBF), Color(0x080B0C10)), startY = roofY, endY = bumperY))

    // ---------- rear glass ----------
    val glass = Path().apply {
        moveTo(cx - cabinHalf * 0.82f, beltY - h * 0.045f)
        cubicTo(cx - cabinHalf * 0.62f, h * 0.44f,
                cx - roofHalf * 1.05f, roofY + h * 0.035f,
                cx - roofHalf * 0.72f, roofY + h * 0.028f)
        lineTo(cx + roofHalf * 0.72f, roofY + h * 0.028f)
        cubicTo(cx + roofHalf * 1.05f, roofY + h * 0.035f,
                cx + cabinHalf * 0.62f, h * 0.44f,
                cx + cabinHalf * 0.82f, beltY - h * 0.045f)
        cubicTo(cx * 1f + cabinHalf * 0.3f, beltY - h * 0.058f,
                cx - cabinHalf * 0.3f, beltY - h * 0.058f,
                cx - cabinHalf * 0.82f, beltY - h * 0.045f)
        close()
    }
    drawPath(glass, GLASS_HI.copy(alpha = 0.10f))
    drawPath(glass, GLASS_HI.copy(alpha = 0.55f), style = Stroke(width = 1.6f))

    // ---------- beltline hint ----------
    drawLine(bright.copy(alpha = 0.35f),
        Offset(cx - beltHalf * 0.92f, beltY + h * 0.004f),
        Offset(cx + beltHalf * 0.92f, beltY + h * 0.004f), strokeWidth = 1.5f)

    // ---------- full-width light bar ----------
    val barY = h * 0.585f
    val barW = beltHalf * 1.72f
    val barH = h * 0.026f
    drawRoundRect(
        Twende.Magenta.copy(alpha = if (moving) 0.35f else 0.18f),
        topLeft = Offset(cx - barW / 2f - 4f, barY - 4f),
        size = Size(barW + 8f, barH + 8f),
        cornerRadius = CornerRadius(10f),
    )
    drawRoundRect(
        Brush.horizontalGradient(listOf(Twende.Magenta, Color(0xFFFF5FAB), Twende.Magenta)),
        topLeft = Offset(cx - barW / 2f, barY),
        size = Size(barW, barH),
        cornerRadius = CornerRadius(7f),
    )

    // ---------- number plate ----------
    drawRoundRect(
        bright.copy(alpha = 0.5f),
        topLeft = Offset(cx - w * 0.052f, h * 0.655f),
        size = Size(w * 0.104f, h * 0.034f),
        cornerRadius = CornerRadius(4f),
        style = Stroke(width = 1.5f),
    )

    // ---------- wheels: arch cuts + tyre contact ----------
    for (sgn in listOf(-1f, 1f)) {
        val axc = cx + sgn * bodyHalf * 0.78f
        // arch (upper half-circle)
        drawArc(
            color = bright.copy(alpha = 0.75f),
            startAngle = 180f, sweepAngle = 180f, useCenter = false,
            topLeft = Offset(axc - w * 0.085f, bumperY - h * 0.062f),
            size = Size(w * 0.17f, h * 0.125f),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )
        // tyre: thick dark stroke to the ground with a bright contact line
        drawLine(Color(0xFF0D1118),
            Offset(axc - w * 0.052f, groundY - h * 0.006f),
            Offset(axc + w * 0.052f, groundY - h * 0.006f),
            strokeWidth = h * 0.045f, cap = StrokeCap.Round)
        drawLine(bright.copy(alpha = 0.55f),
            Offset(axc - w * 0.05f, groundY + h * 0.014f),
            Offset(axc + w * 0.05f, groundY + h * 0.014f),
            strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}
