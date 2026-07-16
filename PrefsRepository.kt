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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.BodyStatus
import co.nedlink.twende.model.Door
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.min
import kotlin.math.roundToInt

private val CARDS = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
private fun cardinal(deg: Float) = CARDS[(((deg + 22.5f) / 45f).toInt()) % 8]

private val WARN = Color(0xFFFF3D7F)
private val PANEL = Color(0xFF12202B)

/**
 * The middle-of-dashboard car. Its road scrolls in proportion to the *real*
 * speed the head unit reads over OBD-II, so the car visibly "moves" with the
 * vehicle. Any open door/hood/boot turns warning-pink and swings out. Tapping a
 * quadrant toggles that panel by hand (bench/manual use); on hardware with a
 * real door bus, CarBodyRepository feeds live state instead.
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

    // Frame-based scroll so lane speed tracks actual km/h (paused when stopped).
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last != 0L) {
                    val dt = (t - last) / 1_000_000_000f
                    phase += speed.value * dt * 8f
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
            drawScene(phase, speed.value, body, glow)
        }

        Text(
            "${cardinal(heading)}  ${heading.roundToInt()}°",
            color = Twende.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        )

        Text(
            "$speedKmh",
            color = Twende.Cyan, fontSize = 30.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 4.dp),
        )
        Text(
            "km/h",
            color = Twende.Dim, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 56.dp, bottom = 11.dp),
        )

        if (body.anyOpen) {
            val label = if (body.openCount == 1) "DOOR OPEN" else "${body.openCount} OPEN"
            Text(
                "⚠ $label",
                color = WARN, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp),
            )
        }
    }
}

private fun zoneFor(x: Float, y: Float, w: Float, h: Float): Door {
    val xf = x / w; val yf = y / h
    return when {
        yf < 0.20f -> Door.HOOD
        yf > 0.80f -> Door.TRUNK
        xf < 0.5f && yf < 0.5f -> Door.FRONT_LEFT
        xf >= 0.5f && yf < 0.5f -> Door.FRONT_RIGHT
        xf < 0.5f -> Door.REAR_LEFT
        else -> Door.REAR_RIGHT
    }
}

private fun DrawScope.drawScene(phase: Float, speedKmh: Int, body: BodyStatus, glow: Float) {
    val w = size.width; val h = size.height
    val moving = speedKmh > 1
    val cyan = Twende.Cyan

    // scrolling lane dashes
    val period = h * 0.22f
    val dashLen = period * 0.55f
    val off = phase % period
    listOf(0.18f, 0.5f, 0.82f).forEach { lx ->
        val x = w * lx
        var y = -period + off
        while (y < h) {
            drawLine(cyan.copy(alpha = 0.16f), Offset(x, y), Offset(x, y + dashLen), strokeWidth = 2f)
            y += period
        }
    }

    val minDim = min(w, h)
    val carW = (w * 0.46f).coerceAtMost(minDim * 0.62f)
    val carH = (h * 0.66f).coerceAtMost(carW * 2.3f)
    val cx = w / 2f
    val left = cx - carW / 2f
    val top = (h - carH) / 2f
    val right = left + carW

    // wheels
    val ww = carW * 0.16f; val wh = carH * 0.14f
    listOf(
        Offset(left - ww * 0.4f, top + carH * 0.12f),
        Offset(right - ww * 0.6f, top + carH * 0.12f),
        Offset(left - ww * 0.4f, top + carH * 0.74f),
        Offset(right - ww * 0.6f, top + carH * 0.74f),
    ).forEach {
        drawRoundRect(
            Color(0xFF05070A), topLeft = it, size = Size(ww, wh),
            cornerRadius = CornerRadius(ww * 0.4f), alpha = if (moving) 0.55f else 1f,
        )
    }

    // body + neon rim (rim brightens with glow while moving)
    val bodyBrush = Brush.verticalGradient(
        listOf(Color(0xFF16202B), Color(0xFF0E151D), Color(0xFF141D27)),
        startY = top, endY = top + carH,
    )
    drawRoundRect(bodyBrush, topLeft = Offset(left, top), size = Size(carW, carH), cornerRadius = CornerRadius(carW * 0.28f))
    drawRoundRect(
        cyan.copy(alpha = 0.55f + 0.35f * if (moving) glow else 0f),
        topLeft = Offset(left, top), size = Size(carW, carH),
        cornerRadius = CornerRadius(carW * 0.28f), style = Stroke(width = 2f),
    )

    // cabin
    val cabW = carW * 0.68f; val cabH = carH * 0.40f
    drawRoundRect(Color(0xFF0A1018), topLeft = Offset(cx - cabW / 2f, top + carH * 0.30f), size = Size(cabW, cabH), cornerRadius = CornerRadius(carW * 0.14f))
    drawRoundRect(cyan.copy(alpha = 0.10f), topLeft = Offset(cx - cabW * 0.4f, top + carH * 0.33f), size = Size(cabW * 0.8f, cabH * 0.42f), cornerRadius = CornerRadius(carW * 0.10f))

    // hood + boot
    drawPanel(body.hood, left + carW * 0.15f, top + carH * 0.035f, carW * 0.70f, carH * 0.10f, -carH * 0.05f, cyan)
    drawPanel(body.trunk, left + carW * 0.15f, top + carH * 0.865f, carW * 0.70f, carH * 0.10f, carH * 0.05f, cyan)

    // doors
    val doorW = carW * 0.15f; val doorLen = carH * 0.20f
    drawDoor(body.frontLeft, left, top + carH * 0.30f, doorW, doorLen, true, cyan)
    drawDoor(body.frontRight, right, top + carH * 0.30f, doorW, doorLen, false, cyan)
    drawDoor(body.rearLeft, left, top + carH * 0.52f, doorW, doorLen, true, cyan)
    drawDoor(body.rearRight, right, top + carH * 0.52f, doorW, doorLen, false, cyan)

    // head/tail lights
    val lw = carW * 0.22f; val lh = carH * 0.02f
    drawRoundRect(cyan, topLeft = Offset(left + carW * 0.16f, top), size = Size(lw, lh), cornerRadius = CornerRadius(2f), alpha = if (moving) 1f else 0.6f)
    drawRoundRect(cyan, topLeft = Offset(left + carW * 0.62f, top), size = Size(lw, lh), cornerRadius = CornerRadius(2f), alpha = if (moving) 1f else 0.6f)
    drawRoundRect(Twende.Magenta, topLeft = Offset(left + carW * 0.16f, top + carH - lh), size = Size(lw, lh), cornerRadius = CornerRadius(2f))
    drawRoundRect(Twende.Magenta, topLeft = Offset(left + carW * 0.62f, top + carH - lh), size = Size(lw, lh), cornerRadius = CornerRadius(2f))
}

private fun DrawScope.drawDoor(open: Boolean, hingeX: Float, hingeY: Float, w: Float, h: Float, leftSide: Boolean, cyan: Color) {
    val angle = if (open) (if (leftSide) -44f else 44f) else 0f
    val x = if (leftSide) hingeX - w else hingeX
    rotate(angle, pivot = Offset(hingeX, hingeY)) {
        drawRoundRect(if (open) WARN else PANEL, topLeft = Offset(x, hingeY), size = Size(w, h), cornerRadius = CornerRadius(w * 0.3f))
        drawRoundRect(if (open) WARN else cyan.copy(alpha = 0.5f), topLeft = Offset(x, hingeY), size = Size(w, h), cornerRadius = CornerRadius(w * 0.3f), style = Stroke(width = 1.4f))
    }
}

private fun DrawScope.drawPanel(open: Boolean, x: Float, y: Float, w: Float, h: Float, openDy: Float, cyan: Color) {
    val yy = if (open) y + openDy else y
    drawRoundRect(if (open) WARN else PANEL, topLeft = Offset(x, yy), size = Size(w, h), cornerRadius = CornerRadius(h * 0.4f))
    drawRoundRect(if (open) WARN else cyan.copy(alpha = 0.5f), topLeft = Offset(x, yy), size = Size(w, h), cornerRadius = CornerRadius(h * 0.4f), style = Stroke(width = 1.4f))
}
