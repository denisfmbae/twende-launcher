package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.Telemetry
import co.nedlink.twende.model.TripStats
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.ui.theme.neonStyle
import kotlin.math.roundToInt

@Composable
fun TelemetryCluster(
    t: Telemetry,
    trip: TripStats,
    metric: Boolean,
    glow: Float,
    speedLimitKmh: Int,
    tankLitres: Float,
    modifier: Modifier = Modifier,
) {
    val speed = if (metric) t.speedKmh else (t.speedKmh * 0.6214f).roundToInt()
    val unit = if (metric) "km/h" else "mph"
    val over = speedLimitKmh > 0 && t.speedKmh > speedLimitKmh
    val speedColor = if (over) FuelRed else Twende.Cyan

    val fuel = t.fuelPct.coerceIn(0, 100)
    val fColor = fuelColor(fuel)
    val litresLeft = fuel / 100f * tankLitres

    Column(
        modifier.glass().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("TELEMETRY · ${t.source}", fontSize = 9.sp, color = Twende.Dim, letterSpacing = 2.sp)

        Text("$speed", style = neonStyle(speedColor, 48, glow),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            if (over) "$unit · OVER $speedLimitKmh" else unit,
            fontSize = 11.sp, color = if (over) FuelRed else Twende.Dim,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )

        // RPM arc — 0..7000
        Canvas(Modifier.fillMaxWidth().height(44.dp)) {
            val stroke = Stroke(width = 9f, cap = StrokeCap.Round)
            val sweepMax = 260f
            drawArc(Twende.Line, 140f, sweepMax, false, style = stroke,
                size = size.copy(height = size.height * 2), topLeft = Offset(0f, 4f))
            drawArc(Twende.Cyan, 140f, sweepMax * (t.rpm / 7000f).coerceIn(0f, 1f), false, style = stroke,
                size = size.copy(height = size.height * 2), topLeft = Offset(0f, 4f))
        }
        StatRow("RPM", "${t.rpm}", Twende.Cyan)

        // ---- Fuel: segmented bar, green -> yellow -> orange -> red ----
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("FUEL", fontSize = 10.sp, color = Twende.Dim, letterSpacing = 1.sp)
            Text("$fuel%  ${fuelWord(fuel)}", fontSize = 12.sp, color = fColor)
        }
        FuelBar(fuelPct = fuel)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${"%.1f".format(litresLeft)} L left", fontSize = 9.sp, color = Twende.Dim)
            Text("~${trip.rangeKm} km range", fontSize = 9.sp,
                color = if (trip.rangeKm in 1..59) FuelOrange else Twende.Dim)
        }

        StatRow("ENGINE °C", "${t.coolantC}°",
            if (t.coolantC > 105) Twende.Magenta else Twende.Cyan)

        if (t.batteryV > 1f) {
            StatRow(
                "BATTERY",
                "${"%.1f".format(t.batteryV)} V",
                if (t.batteryWarning) FuelRed else FuelGreen,
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 10.sp, color = Twende.Dim)
        Text(value, fontSize = 13.sp, color = color)
    }
}
