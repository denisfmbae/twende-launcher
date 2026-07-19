package co.nedlink.twende.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.DtcReport
import co.nedlink.twende.model.Telemetry
import co.nedlink.twende.model.TripStats
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import kotlin.math.roundToInt

/**
 * The numbers a driver actually acts on. Range/cost/eco/idle are derived from real
 * telemetry; the check-engine tile reads genuine fault codes from the car over
 * OBD-II Mode 03 — tap it to scan.
 */
@Composable
fun VitalsStrip(
    trip: TripStats,
    t: Telemetry,
    dtc: DtcReport,
    scanning: Boolean,
    priceSet: Boolean,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        Vital(
            label = "RANGE",
            value = if (trip.rangeKm > 0) "${trip.rangeKm} km" else "—",
            sub = if (trip.consumptionL100 > 0.5f) "${"%.1f".format(trip.consumptionL100)} L/100km" else "learning…",
            color = if (trip.rangeKm in 1..59) FuelOrange else Twende.Cyan,
            modifier = Modifier.weight(1f),
        )

        Vital(
            label = "TRIP",
            value = if (priceSet) "KES ${trip.costKes.roundToInt()}" else "${"%.2f".format(trip.litresUsed)} L",
            sub = "${"%.1f".format(trip.distanceKm)} km" + if (priceSet) "" else " · set KES/L",
            color = Twende.Cyan,
            modifier = Modifier.weight(1f),
        )

        Vital(
            label = "ECO",
            value = "${trip.ecoScore}",
            sub = "${trip.harshEvents} harsh",
            color = when {
                trip.ecoScore >= 80 -> FuelGreen
                trip.ecoScore >= 60 -> FuelYellow
                else -> FuelOrange
            },
            modifier = Modifier.weight(1f),
        )

        Vital(
            label = "IDLE",
            value = if (priceSet) "KES ${trip.idleCostKes.roundToInt()}" else "${trip.idleSeconds.roundToInt()}s",
            sub = if (priceSet) "${(trip.idleSeconds / 60).roundToInt()} min wasted" else "engine idling",
            color = if (trip.idleCostKes > 30f) FuelOrange else Twende.Dim,
            modifier = Modifier.weight(1f),
        )

        val faults = dtc.codes.size
        Vital(
            label = "CHECK ENGINE",
            value = when {
                scanning -> "Scanning…"
                !dtc.scanned -> "Tap to scan"
                !dtc.connected -> "No adapter"
                faults == 0 -> "No faults"
                else -> "$faults code" + if (faults > 1) "s" else ""
            },
            sub = when {
                !dtc.scanned -> "reads real codes"
                !dtc.connected -> "plug in ELM327"
                faults > 0 -> dtc.codes.first().code
                dtc.milOn -> "lamp on, no codes"
                else -> "all clear"
            },
            color = if (dtc.scanned && faults > 0) FuelRed else if (dtc.scanned && dtc.connected) FuelGreen else Twende.Cyan,
            modifier = Modifier.weight(1.3f),
            onClick = onScan,
        )
    }
}

@Composable
private fun Vital(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val base = if (onClick != null) modifier.clickable { onClick() } else modifier
    Column(base.glass(14).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, fontSize = 8.sp, letterSpacing = 1.5.sp, color = Twende.Dim, maxLines = 1)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(sub, fontSize = 8.sp, color = Twende.Dim, maxLines = 1)
    }
}

/** Full fault detail, shown under the strip once a scan finds codes. */
@Composable
fun DtcPanel(dtc: DtcReport, modifier: Modifier = Modifier) {
    if (!dtc.scanned || dtc.codes.isEmpty()) return
    Column(modifier.fillMaxWidth().glass(14).padding(10.dp)) {
        Text(
            if (dtc.simulated) "⚠ CHECK ENGINE (simulated demo)" else "⚠ CHECK ENGINE LAMP ON",
            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FuelRed, letterSpacing = 1.sp,
        )
        dtc.codes.take(3).forEach { d ->
            Text(
                "${d.code} — ${d.desc}",
                fontSize = 10.sp, color = Twende.Ink, maxLines = 1,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}
