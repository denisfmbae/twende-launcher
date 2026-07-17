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
    // Bright rim regardless of motion so the car always reads clearly; brighter when moving.
    val rim = Twende.Cyan.copy(alpha = 0.75f + 0.25f * if (moving) g else 0.4f)

    // Bigger car: fills more of the widget than before.
    val rearY = h * 0.74f
    val rearTopY = h * 0.44f
    val roofFarY = h * 0.30f
    val rearHalf = w * 0.30f
    val frontHalf = w * 0.17f

    // Glossy vertical gradient — lit roof down to shadowed sills.
    val bodyBrush = Brush.verticalGradient(
        0f to BODY_TOP, 0.45f to BODY_MID, 1f to BODY_LOW,
        startY = roofFarY, endY = rearY,
    )

    // Contact shadow under the car for grounding.
    drawOval(
        Brush.radialGradient(
            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
            center = Offset(cx, rearY + h * 0.015f), radius = w * 0.34f,
        ),
        topLeft = Offset(cx - w * 0.34f, rearY - h * 0.03f),
        size = Size(w * 0.68f, h * 0.12f),
    )

    // ---- body silhouette (rear face nearest, roof receding forward) ----
    val bodyPath = Path().apply {
        moveTo(cx - rearHalf, rearY)
        lineTo(cx - rearHalf, rearTopY)
        lineTo(cx - frontHalf, roofFarY)
        lineTo(cx + frontHalf, roofFarY)
        lineTo(cx + rearHalf, rearTopY)
        lineTo(cx + rearHalf, rearY)
        close()
    }
    drawPath(bodyPath, bodyBrush, style = Fill)
    drawPath(bodyPath, rim, style = Stroke(width = 3f))

    // top highlight sheen across the roof edge
    drawLine(
        GLASS_HI.copy(alpha = 0.55f),
        Offset(cx - frontHalf, roofFarY), Offset(cx + frontHalf, roofFarY),
        strokeWidth = 2.5f,
    )

    // ---- rear windscreen (bright, clear) ----
    val glassPath = Path().apply {
        val gTop = rearTopY + (roofFarY - rearTopY) * 0.18f
        moveTo(cx - rearHalf * 0.70f, rearTopY + h * 0.015f)
        lineTo(cx - frontHalf * 0.68f, gTop)
        lineTo(cx + frontHalf * 0.68f, gTop)
        lineTo(cx + rearHalf * 0.70f, rearTopY + h * 0.015f)
        close()
    }
    drawPath(
        glassPath,
        Brush.verticalGradient(listOf(GLASS_HI.copy(alpha = 0.35f), Twende.Cyan.copy(alpha = 0.12f))),
    )
    drawPath(glassPath, GLASS_HI.copy(alpha = 0.55f), style = Stroke(width = 1.5f))

    // ---- rear pillars accent ----
    drawLine(rim.copy(alpha = 0.5f), Offset(cx - rearHalf, rearTopY), Offset(cx - rearHalf, rearY), strokeWidth = 2f)
    drawLine(rim.copy(alpha = 0.5f), Offset(cx + rearHalf, rearTopY), Offset(cx + rearHalf, rearY), strokeWidth = 2f)

    // ---- vivid taillight bar across the rear ----
    val tailY = rearY - h * 0.06f
    drawRoundRect(
        Twende.Magenta.copy(alpha = if (moving) 0.35f else 0.18f),
        topLeft = Offset(cx - rearHalf * 1.02f, tailY - h * 0.015f),
        size = Size(rearHalf * 2.04f, h * 0.075f),
        cornerRadius = CornerRadius(8f),
    )
    drawRoundRect(
        Brush.horizontalGradient(listOf(Twende.Magenta, Color(0xFFFF4FA3), Twende.Magenta)),
        topLeft = Offset(cx - rearHalf * 0.92f, tailY),
        size = Size(rearHalf * 1.84f, h * 0.032f),
        cornerRadius = CornerRadius(5f),
    )

    // ---- open panels: warning pink overlays ----
    drawSideDoor(body.rearLeft, true, true, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.frontLeft, true, false, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.rearRight, false, true, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.frontRight, false, false, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)

    if (body.trunk) {
        drawRoundRect(WARN.copy(alpha = 0.6f),
            topLeft = Offset(cx - rearHalf * 0.8f, rearTopY + h * 0.02f),
            size = Size(rearHalf * 1.6f, h * 0.05f), cornerRadius = CornerRadius(6f))
    }
    if (body.hood) {
        drawRoundRect(WARN.copy(alpha = 0.6f),
            topLeft = Offset(cx - frontHalf * 0.9f, roofFarY - h * 0.02f),
            size = Size(frontHalf * 1.8f, h * 0.04f), cornerRadius = CornerRadius(6f))
    }
}

private fun DrawScope.drawSideDoor(
    open: Boolean, left: Boolean, near: Boolean,
    cx: Float, rearHalf: Float, frontHalf: Float,
    rearY: Float, rearTopY: Float, roofFarY: Float,
) {
    if (!open) return
    val sign = if (left) -1f else 1f
    val midHalf = (rearHalf + frontHalf) / 2f
    val midBottomY = (rearY + roofFarY) / 2f + (rearTopY - roofFarY) * 0.15f
    val midTopY = (rearTopY + roofFarY) / 2f

    val xA: Float; val botA: Float; val topA: Float
    val xB: Float; val botB: Float; val topB: Float
    if (near) {
        xA = sign * rearHalf; botA = rearY; topA = rearTopY
        xB = sign * midHalf;  botB = midBottomY; topB = midTopY
    } else {
        xA = sign * midHalf;  botA = midBottomY; topA = midTopY
        xB = sign * frontHalf; botB = roofFarY;  topB = roofFarY
    }
    val path = Path().apply {
        moveTo(cx + xA, botA)
        lineTo(cx + xA, topA)
        lineTo(cx + xB, topB)
        lineTo(cx + xB, botB)
        close()
    }
    drawPath(path, WARN.copy(alpha = 0.6f))
    drawPath(path, WARN, style = Stroke(width = 2.5f))
}
