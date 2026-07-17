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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.BodyStatus
import co.nedlink.twende.model.Door
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.roundToInt

private val CARDS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private fun cardinal(deg: Float) = CARDS[(((deg + 22.5f) / 45f).toInt()) % 8]

private val WARN = Color(0xFFFF3D7F)
private val BODY_HI = Color(0xFF1E2A63)
private val BODY_LO = Color(0xFF0A1230)

/**
 * The centre car, drawn as a 3/4-rear perspective view — looking at the back of
 * the car as it drives away, tilted into the screen, the way a real digital
 * cluster shows it. The floor grid recedes to a vanishing point and scrolls
 * toward the viewer in proportion to real OBD speed. Open doors/hood/boot glow
 * warning-pink. Same signature as before, so HomeScreen is unchanged.
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

    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L) {
                    val dt = (t - last) / 1_000_000_000f
                    phase += speed.value * dt * 0.9f
                    if (phase > 100_000f) phase -= 100_000f
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
            drawCarPerspective(body, speed.value, glow)
        }

        Text(
            "${cardinal(heading)}  ${heading.roundToInt()}°",
            color = Twende.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Text(
            "$speedKmh",
            color = Twende.Cyan, fontSize = 40.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 6.dp),
        )
        Text(
            "km/h",
            color = Twende.Dim, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 78.dp, bottom = 14.dp),
        )
        if (body.anyOpen) {
            val label = if (body.openCount == 1) "DOOR OPEN" else "${body.openCount} OPEN"
            Text(
                "⚠ $label",
                color = WARN, fontSize = 14.sp, fontWeight = FontWeight.Bold,
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
    val horizon = h * 0.30f
    val vpx = w / 2f
    val baseA = 0.12f + 0.10f * glow.coerceIn(0f, 1f)
    val cyan = Twende.Cyan.copy(alpha = baseA)

    for (side in listOf(-1f, 1f)) {
        val nearX = vpx + side * w * 0.6f
        drawLine(cyan, Offset(vpx, horizon), Offset(nearX, h), strokeWidth = 2f)
        val innerX = vpx + side * w * 0.48f
        drawLine(cyan.copy(alpha = baseA * 0.7f), Offset(vpx, horizon), Offset(innerX, h), strokeWidth = 1.5f)
    }

    val bands = 10
    for (i in 0..bands) {
        val p = ((i + (phase % 1f)) / bands).coerceIn(0f, 1f)
        val y = horizon + (h - horizon) * (p * p)
        drawLine(Twende.Cyan.copy(alpha = baseA * (0.3f + 0.7f * p)), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
    }
}

private fun DrawScope.drawCarPerspective(body: BodyStatus, speedKmh: Int, glow: Float) {
    val w = size.width; val h = size.height
    val cx = w / 2f
    val moving = speedKmh > 1
    val rim = Twende.Cyan.copy(alpha = 0.6f + 0.4f * if (moving) glow.coerceIn(0f, 1f) else 0f)

    val rearY = h * 0.78f
    val rearTopY = h * 0.50f
    val roofFarY = h * 0.38f
    val rearHalf = w * 0.26f
    val frontHalf = w * 0.15f

    val bodyBrush = Brush.verticalGradient(listOf(BODY_HI, BODY_LO), startY = roofFarY, endY = rearY)

    drawOval(
        Brush.radialGradient(
            listOf(Twende.Cyan.copy(alpha = 0.18f), Color.Transparent),
            center = Offset(cx, rearY + h * 0.02f), radius = w * 0.42f,
        ),
        topLeft = Offset(cx - w * 0.42f, rearY - h * 0.06f),
        size = Size(w * 0.84f, h * 0.20f),
    )

    val bodyPath = Path().apply {
        moveTo(cx - rearHalf, rearY)
        lineTo(cx - rearHalf, rearTopY)
        lineTo(cx - frontHalf, roofFarY)
        lineTo(cx + frontHalf, roofFarY)
        lineTo(cx + rearHalf, rearTopY)
        lineTo(cx + rearHalf, rearY)
        close()
    }
    drawPath(bodyPath, bodyBrush)
    drawPath(bodyPath, rim, style = Stroke(width = 2.5f))

    val glassPath = Path().apply {
        val gTop = rearTopY + (roofFarY - rearTopY) * 0.15f
        moveTo(cx - rearHalf * 0.72f, rearTopY + h * 0.02f)
        lineTo(cx - frontHalf * 0.7f, gTop)
        lineTo(cx + frontHalf * 0.7f, gTop)
        lineTo(cx + rearHalf * 0.72f, rearTopY + h * 0.02f)
        close()
    }
    drawPath(glassPath, Twende.Cyan.copy(alpha = 0.10f))
    drawPath(glassPath, Twende.Cyan.copy(alpha = 0.30f), style = Stroke(width = 1.5f))

    val tail = if (moving) Twende.Magenta else Twende.Magenta.copy(alpha = 0.6f)
    drawRoundRect(
        tail,
        topLeft = Offset(cx - rearHalf * 0.9f, rearY - h * 0.05f),
        size = Size(rearHalf * 1.8f, h * 0.035f),
        cornerRadius = CornerRadius(4f),
    )

    drawSideDoor(body.rearLeft, true, true, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.frontLeft, true, false, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.rearRight, false, true, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)
    drawSideDoor(body.frontRight, false, false, cx, rearHalf, frontHalf, rearY, rearTopY, roofFarY)

    if (body.trunk) {
        drawRoundRect(WARN.copy(alpha = 0.5f),
            topLeft = Offset(cx - rearHalf * 0.8f, rearTopY + h * 0.02f),
            size = Size(rearHalf * 1.6f, h * 0.05f), cornerRadius = CornerRadius(6f))
    }
    if (body.hood) {
        drawRoundRect(WARN.copy(alpha = 0.5f),
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
    drawPath(path, WARN.copy(alpha = 0.55f))
    drawPath(path, WARN, style = Stroke(width = 2f))
}
