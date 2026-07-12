package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.Telemetry
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.ui.theme.neonStyle
import kotlin.math.roundToInt

@Composable
fun TelemetryCluster(t: Telemetry, metric: Boolean, glow: Float, modifier: Modifier = Modifier) {
    val speed = if (metric) t.speedKmh else (t.speedKmh * 0.6214f).roundToInt()
    val unit = if (metric) "km/h" else "mph"
    val fuelColor = if (t.fuelPct < 25) Twende.Magenta else Twende.Cyan

    Column(modifier.glass().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("TELEMETRY · ${t.source}", fontSize = 9.sp, color = Twende.Dim, letterSpacing = 2.sp)

        Text("$speed", style = neonStyle(Twende.Cyan, 52, glow), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(unit, fontSize = 11.sp, color = Twende.Dim, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        // RPM arc — 0..7000
        Canvas(Modifier.fillMaxWidth().height(52.dp)) {
            val stroke = Stroke(width = 9f, cap = StrokeCap.Round)
            val sweepMax = 260f
            drawArc(Twende.Line, 140f, sweepMax, false, style = stroke,
                size = size.copy(height = size.height * 2), topLeft = androidx.compose.ui.geometry.Offset(0f, 4f))
            drawArc(Twende.Cyan, 140f, sweepMax * (t.rpm / 7000f).coerceIn(0f, 1f), false, style = stroke,
                size = size.copy(height = size.height * 2), topLeft = androidx.compose.ui.geometry.Offset(0f, 4f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RPM", fontSize = 10.sp, color = Twende.Dim)
            Text("${t.rpm}", fontSize = 12.sp, color = Twende.Cyan)
        }

        // Fuel bar
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("FUEL", fontSize = 10.sp, color = Twende.Dim)
            Text("${t.fuelPct}%", fontSize = 12.sp, color = fuelColor)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(Twende.Line, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            drawRoundRect(fuelColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
                size = size.copy(width = size.width * (t.fuelPct / 100f).coerceIn(0f, 1f)))
        }

        Spacer(Modifier.height(0.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ENGINE °C", fontSize = 10.sp, color = Twende.Dim)
            Text("${t.coolantC}°", fontSize = 14.sp,
                color = if (t.coolantC > 105) Twende.Magenta else Twende.Cyan)
        }
    }
}
