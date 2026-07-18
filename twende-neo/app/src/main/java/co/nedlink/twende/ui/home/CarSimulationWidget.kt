package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.BodyStatus
import co.nedlink.twende.model.Door
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.roundToInt
import kotlin.math.sin

private val CARDS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private fun cardinal(deg: Float) = CARDS[(((deg + 22.5f) / 45f).toInt()) % 8]

private val WARN = Color(0xFFFF3D7F)

// Brighter body palette — a lit car, not a dark blob. Cool steel-blue with a
// bright top highlight so the roof/hood catch "light" the way the reference does.
private val BODY_TOP = Color(0xFF4A6BFF)   // lit upper surfaces
private val BODY_MID = Color(0xFF243A9E)   // mid body
private val BODY_LOW = Color(0xFF0E1740)   // shadowed sills
private val GLASS_HI = Color(0xFF9FE8FF)   // bright glass edge
private val HALO = Color(0xFF39B9FF)        // base glow ring

/**
 * The centre car, drawn as a bright 3/4-rear perspective view — looking at the
 * back of the car as it drives away, tilted into the screen, sitting on a
 * glowing circular halo like a premium digital cluster. Redrawn to be larger,
 * brighter and higher-contrast (matching the reference dash): glossy body
 * gradient, strong rim light, clear windscreen and a vivid taillight bar.
 * Floor grid still recedes to a vanishing point and scrolls with real speed.
 * Open doors/hood/boot glow warning-pink. Same signature, so HomeScreen is
 * unchanged.
 */
@Composable
fun CarSimulationWidget(
    speedKmh: Int,
    heading: Float,
    body: BodyStatus,
    glow: Float,
    onToggle: (Door) -> Unit,
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
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { o ->
                        onToggle(zoneFor(o.x, o.y, size.width.toFloat(), size.height.toFloat()))
                    }
                },
        ) {
            drawFloor(phase, glow)
            drawHalo(glow, pulse)
            drawCarPerspective(body, speed.value, glow)
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
        if (body.anyOpen) {
            val label = if (body.openCount == 1) "DOOR OPEN" else "${body.openCount} OPEN"
            Text(
                "⚠ $label",
                color = WARN, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
        }
    }
}

private fun zoneFor(x: Float, y: Float, w: Float, h: Float): Door {
    val xf = x / w; val yf = y / h
    return when {
        yf < 0.28f -> Door.HOOD
        yf > 0.80f -> Door.TRUNK
        xf < 0.5f && yf < 0.55f -> Door.FRONT_LEFT
        xf >= 0.5f && yf < 0.55f -> Door.FRONT_RIGHT
        xf < 0.5f -> Door.REAR_LEFT
        else -> Door.REAR_RIGHT
    }
}

private fun DrawScope.drawFloor(phase: Float, glow: Float) {
    val w = size.width; val h = size.height
    val horizon = h * 0.28f
    val vpx = w / 2f
    val baseA = 0.14f + 0.12f * glow.coerceIn(0f, 1f)
    val cyan = Twende.Cyan.copy(alpha = baseA)

    for (side in listOf(-1f, 1f)) {
        val nearX = vpx + side * w * 0.62f
        drawLine(cyan, Offset(vpx, horizon), Offset(nearX, h), strokeWidth = 2f)
        val innerX = vpx + side * w * 0.46f
        drawLine(cyan.copy(alpha = baseA * 0.6f), Offset(vpx, horizon), Offset(innerX, h), strokeWidth = 1.5f)
    }

    val bands = 10
    for (i in 0..bands) {
        val p = ((i + (phase % 1f)) / bands).coerceIn(0f, 1f)
        val y = horizon + (h - horizon) * (p * p)
        drawLine(Twende.Cyan.copy(alpha = baseA * (0.25f + 0.6f * p)), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
}

/** The glowing circular base the car sits on — the signature premium-cluster look. */
private fun DrawScope.drawHalo(glow: Float, pulse: Float) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val cy = h * 0.66f
    val breathe = 0.85f + 0.15f * sin(pulse * 2.2f)
    val g = glow.coerceIn(0f, 1f)

    // soft outer bloom
    drawOval(
        Brush.radialGradient(
            listOf(HALO.copy(alpha = 0.28f * breathe * (0.6f + 0.4f * g)), Color.Transparent),
            center = Offset(cx, cy), radius = w * 0.44f,
        ),
        topLeft = Offset(cx - w * 0.46f, cy - h * 0.16f),
        size = Size(w * 0.92f, h * 0.34f),
    )
    // crisp ellipse stroke = the disc edge
    val ringPath = Path().apply {
        addOval(Rect(Offset(cx - w * 0.33f, cy - h * 0.085f), Size(w * 0.66f, h * 0.19f)))
    }
    drawPath(ringPath, Twende.Cyan.copy(alpha = 0.55f * breathe), style = Stroke(width = 2.5f))
    drawPath(ringPath, GLASS_HI.copy(alpha = 0.20f), style = Stroke(width = 6f))
}

private fun DrawScope.drawCarPerspective(body: BodyStatus, speedKmh: Int, glow: Float) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val moving = speedKmh > 1
    val g = glow.coerceIn(0f, 1f)
    val rim = Twende.Cyan.copy(alpha = 0.80f + 0.20f * if (moving) g else 0.4f)

    // Layout — a car seen from directly behind, roof receding away from you.
    val groundY  = h * 0.78f     // where the tyres meet the road
    val bodyBotY = h * 0.68f     // bottom of the painted body
    val beltY    = h * 0.50f     // beltline: body below, glass above
    val roofY    = h * 0.33f     // top of the rear pillars
    val roofFarY = h * 0.25f     // roof receding toward the front
    val bodyHalf  = w * 0.27f
    val cabinHalf = w * 0.205f

    val bodyBrush = Brush.verticalGradient(
        0f to BODY_TOP, 0.55f to BODY_MID, 1f to BODY_LOW,
        startY = beltY, endY = bodyBotY,
    )
    val cabinBrush = Brush.verticalGradient(
        0f to BODY_MID, 1f to BODY_TOP,
        startY = roofY, endY = beltY,
    )

    // ---- contact shadow ----
    drawOval(
        Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent),
            center = Offset(cx, groundY), radius = w * 0.32f,
        ),
        topLeft = Offset(cx - w * 0.32f, groundY - h * 0.035f),
        size = Size(w * 0.64f, h * 0.09f),
    )

    // ---- wheels (drawn first so the body sits over them) ----
    // Wheels are what make a shape read as "a car" at a glance.
    val tyre = Color(0xFF11141B)
    val tyreEdge = Color(0xFF2A3340)
    val wheelW = w * 0.115f
    val wheelTop = bodyBotY - h * 0.055f
    for (sgn in listOf(-1f, 1f)) {
        val left = cx + sgn * bodyHalf * 0.86f - wheelW / 2f
        drawRoundRect(
            tyre,
            topLeft = Offset(left, wheelTop),
            size = Size(wheelW, groundY - wheelTop),
            cornerRadius = CornerRadius(wheelW * 0.36f),
        )
        drawRoundRect(
            tyreEdge,
            topLeft = Offset(left, wheelTop),
            size = Size(wheelW, groundY - wheelTop),
            cornerRadius = CornerRadius(wheelW * 0.36f),
            style = Stroke(width = 2f),
        )
        // hub highlight
        drawRoundRect(
            Twende.Cyan.copy(alpha = 0.18f),
            topLeft = Offset(left + wheelW * 0.28f, wheelTop + (groundY - wheelTop) * 0.30f),
            size = Size(wheelW * 0.44f, (groundY - wheelTop) * 0.34f),
            cornerRadius = CornerRadius(wheelW * 0.2f),
        )
    }

    // ---- roof receding away (gives the 3D read) ----
    val roofPath = Path().apply {
        moveTo(cx - cabinHalf * 0.90f, roofY)
        lineTo(cx - cabinHalf * 0.52f, roofFarY)
        lineTo(cx + cabinHalf * 0.52f, roofFarY)
        lineTo(cx + cabinHalf * 0.90f, roofY)
        close()
    }
    drawPath(roofPath, Brush.verticalGradient(listOf(BODY_LOW, BODY_MID), startY = roofFarY, endY = roofY))
    drawPath(roofPath, rim.copy(alpha = 0.55f), style = Stroke(width = 2f))

    // ---- cabin / greenhouse ----
    val cabinPath = Path().apply {
        moveTo(cx - cabinHalf, beltY)
        lineTo(cx - cabinHalf * 0.90f, roofY)
        lineTo(cx + cabinHalf * 0.90f, roofY)
        lineTo(cx + cabinHalf, beltY)
        close()
    }
    drawPath(cabinPath, cabinBrush)
    drawPath(cabinPath, rim, style = Stroke(width = 2.5f))

    // ---- rear window ----
    val glassPath = Path().apply {
        moveTo(cx - cabinHalf * 0.84f, beltY - h * 0.012f)
        lineTo(cx - cabinHalf * 0.74f, roofY + h * 0.022f)
        lineTo(cx + cabinHalf * 0.74f, roofY + h * 0.022f)
        lineTo(cx + cabinHalf * 0.84f, beltY - h * 0.012f)
        close()
    }
    drawPath(
        glassPath,
        Brush.verticalGradient(listOf(GLASS_HI.copy(alpha = 0.42f), Twende.Cyan.copy(alpha = 0.10f))),
    )
    drawPath(glassPath, GLASS_HI.copy(alpha = 0.6f), style = Stroke(width = 1.5f))

    // ---- lower body ----
    drawRoundRect(
        bodyBrush,
        topLeft = Offset(cx - bodyHalf, beltY),
        size = Size(bodyHalf * 2f, bodyBotY - beltY),
        cornerRadius = CornerRadius(w * 0.035f),
    )
    drawRoundRect(
        rim,
        topLeft = Offset(cx - bodyHalf, beltY),
        size = Size(bodyHalf * 2f, bodyBotY - beltY),
        cornerRadius = CornerRadius(w * 0.035f),
        style = Stroke(width = 2.5f),
    )
    // shoulder highlight
    drawLine(
        GLASS_HI.copy(alpha = 0.45f),
        Offset(cx - bodyHalf * 0.9f, beltY + h * 0.012f),
        Offset(cx + bodyHalf * 0.9f, beltY + h * 0.012f),
        strokeWidth = 2f,
    )

    // ---- tail lights ----
    val lampY = beltY + h * 0.035f
    val lampH = h * 0.052f
    val lampW = bodyHalf * 0.46f
    for (sgn in listOf(-1f, 1f)) {
        val lx = if (sgn < 0) cx - bodyHalf * 0.94f else cx + bodyHalf * 0.94f - lampW
        drawRoundRect(
            Twende.Magenta.copy(alpha = if (moving) 0.32f else 0.16f),
            topLeft = Offset(lx - 3f, lampY - 3f),
            size = Size(lampW + 6f, lampH + 6f),
            cornerRadius = CornerRadius(7f),
        )
        drawRoundRect(
            Brush.horizontalGradient(listOf(Twende.Magenta, Color(0xFFFF5FAB))),
            topLeft = Offset(lx, lampY),
            size = Size(lampW, lampH),
            cornerRadius = CornerRadius(5f),
        )
    }

    // ---- bumper + plate ----
    drawRoundRect(
        Color(0xFF0A1030),
        topLeft = Offset(cx - bodyHalf * 0.99f, bodyBotY - h * 0.075f),
        size = Size(bodyHalf * 1.98f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.02f),
    )
    drawRoundRect(
        rim.copy(alpha = 0.45f),
        topLeft = Offset(cx - bodyHalf * 0.99f, bodyBotY - h * 0.075f),
        size = Size(bodyHalf * 1.98f, h * 0.075f),
        cornerRadius = CornerRadius(w * 0.02f),
        style = Stroke(width = 1.5f),
    )
    drawRoundRect(
        Color(0xFFB9C6D6).copy(alpha = 0.55f),
        topLeft = Offset(cx - w * 0.048f, bodyBotY - h * 0.062f),
        size = Size(w * 0.096f, h * 0.032f),
        cornerRadius = CornerRadius(3f),
    )

    // ---- open panels (demo mode only; nothing is invented otherwise) ----
    if (body.frontLeft || body.rearLeft) drawOpenSide(cx, -1f, bodyHalf, beltY, bodyBotY)
    if (body.frontRight || body.rearRight) drawOpenSide(cx, 1f, bodyHalf, beltY, bodyBotY)
    if (body.trunk) {
        drawRoundRect(WARN.copy(alpha = 0.55f),
            topLeft = Offset(cx - bodyHalf * 0.8f, beltY + h * 0.005f),
            size = Size(bodyHalf * 1.6f, h * 0.03f), cornerRadius = CornerRadius(5f))
    }
    if (body.hood) {
        drawRoundRect(WARN.copy(alpha = 0.55f),
            topLeft = Offset(cx - cabinHalf * 0.5f, roofFarY - h * 0.018f),
            size = Size(cabinHalf, h * 0.028f), cornerRadius = CornerRadius(5f))
    }
}

/** A pink panel on one flank when a door on that side is reported open. */
private fun DrawScope.drawOpenSide(
    cx: Float, sgn: Float, bodyHalf: Float, beltY: Float, bodyBotY: Float,
) {
    val wdt = bodyHalf * 0.30f
    val x = if (sgn < 0) cx - bodyHalf else cx + bodyHalf - wdt
    drawRoundRect(
        WARN.copy(alpha = 0.55f),
        topLeft = Offset(x, beltY),
        size = Size(wdt, bodyBotY - beltY),
        cornerRadius = CornerRadius(6f),
    )
    drawRoundRect(
        WARN,
        topLeft = Offset(x, beltY),
        size = Size(wdt, bodyBotY - beltY),
        cornerRadius = CornerRadius(6f),
        style = Stroke(width = 2.5f),
    )
}
