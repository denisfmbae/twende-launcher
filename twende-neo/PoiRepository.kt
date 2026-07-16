package co.nedlink.twende.ui.home

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.neonStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val CARDINALS = listOf("North", "North-East", "East", "South-East", "South", "South-West", "West", "North-West")
private fun cardinalName(deg: Float) = CARDINALS[(((deg + 22.5f) / 45f).toInt()) % 8]

/**
 * Rotating compass rose. The raw azimuth is unwrapped into a continuous angle
 * so 359°→1° animates through 360°, never a 358° backspin.
 */
@Composable
fun CompassWidget(headingDeg: Float, glow: Float, modifier: Modifier = Modifier) {
    var continuous by remember { mutableFloatStateOf(headingDeg) }
    var last by remember { mutableFloatStateOf(headingDeg) }
    LaunchedEffect(headingDeg) {
        val delta = ((headingDeg - last + 540f) % 360f) - 180f
        continuous += delta
        last = headingDeg
    }
    val animated by animateFloatAsState(
        targetValue = continuous,
        animationSpec = tween(durationMillis = 220, easing = LinearOutSlowInEasing),
        label = "heading",
    )

    val measurer = rememberTextMeasurer()

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(6.dp)
        ) {
            val r = min(size.width, size.height) / 2f
            val c = center

            // outer glow ring
            drawCircle(Twende.Cyan.copy(alpha = 0.10f * glow), radius = r, center = c)
            drawCircle(Twende.Cyan.copy(alpha = 0.55f), radius = r * 0.98f, center = c, style = Stroke(2f))
            drawCircle(Twende.Line, radius = r * 0.72f, center = c, style = Stroke(1f))

            rotate(degrees = -animated, pivot = c) {
                // tick marks every 15°, majors every 90°
                for (a in 0 until 360 step 15) {
                    val major = a % 90 == 0
                    val rad = Math.toRadians(a.toDouble())
                    val outer = Offset(c.x + (r * 0.95f) * sin(rad).toFloat(), c.y - (r * 0.95f) * cos(rad).toFloat())
                    val inner = Offset(c.x + (r * (if (major) 0.82f else 0.89f)) * sin(rad).toFloat(), c.y - (r * (if (major) 0.82f else 0.89f)) * cos(rad).toFloat())
                    drawLine(if (major) Twende.Cyan else Twende.Dim, inner, outer, if (major) 3f else 1.5f)
                }
                // cardinal letters (N in magenta)
                listOf("N" to 0, "E" to 90, "S" to 180, "W" to 270).forEach { (label, angle) ->
                    val rad = Math.toRadians(angle.toDouble())
                    val pos = Offset(c.x + (r * 0.62f) * sin(rad).toFloat(), c.y - (r * 0.62f) * cos(rad).toFloat())
                    val layout = measurer.measure(
                        AnnotatedString(label),
                        neonStyle(if (label == "N") Twende.Magenta else Color(0xFFB9C4CE), 18, glow).copy(fontSize = 18.sp)
                    )
                    drawText(layout, topLeft = Offset(pos.x - layout.size.width / 2f, pos.y - layout.size.height / 2f))
                }
            }

            // fixed lubber needle pointing up
            val needle = Path().apply {
                moveTo(c.x, c.y - r * 0.80f)
                lineTo(c.x - r * 0.06f, c.y - r * 0.58f)
                lineTo(c.x + r * 0.06f, c.y - r * 0.58f)
                close()
            }
            drawPath(needle, Twende.Cyan)
            drawCircle(Twende.Cyan, radius = 5f, center = c)
        }

        val shown = ((headingDeg.roundToInt() % 360) + 360) % 360
        Text("${cardinalName(headingDeg)}  $shown°", style = neonStyle(Twende.Cyan, 20, glow))
    }
}
