package co.nedlink.twende.ui.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.data.sensors.DeviceSensor
import co.nedlink.twende.data.sensors.DeviceTelemetry
import co.nedlink.twende.ui.theme.Twende
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Live readout of the sensors this head unit actually owns. Unlike the OBD scan
 * — which needs a dongle in the car's port and returns nothing without one —
 * every value here comes from hardware inside the unit, so the screen is useful
 * the moment it opens. Rescan re-reads the inventory directly from
 * SensorManager, with no bound service in the path; that indirection is what
 * made the old rescan button appear dead.
 */
@Composable
fun DeviceSensorPanel(
    inventory: List<DeviceSensor>,
    live: DeviceTelemetry,
    onRescan: () -> Unit,
    onClose: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Twende.Cosmic.copy(alpha = 0.97f))
            .padding(18.dp),
    ) {
        Column(Modifier.fillMaxSize()) {

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "UNIT SENSORS",
                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp, color = Twende.Cyan,
                    )
                    Text(
                        "Live from this head unit — no OBD dongle needed",
                        fontSize = 12.sp, color = Twende.Dim,
                    )
                }
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Twende.ButtonBg)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 24.sp, color = Twende.Ink) }
            }

            Spacer(Modifier.height(14.dp))

            // ---- the four headline readings ----
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigStat(
                    "GPS SPEED",
                    if (live.gpsFix) "${live.gpsSpeedKmh}" else "—",
                    if (live.gpsFix) "km/h · true road speed" else "waiting for fix",
                    Modifier.weight(1f),
                )
                BigStat(
                    "G-FORCE",
                    String.format("%.2f", live.gForce),
                    when {
                        live.accelG < -0.15f -> "braking"
                        live.accelG > 0.15f -> "accelerating"
                        abs(live.lateralG) > 0.15f -> "cornering"
                        else -> "steady"
                    },
                    Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigStat(
                    "INCLINE",
                    "${live.tiltDeg.roundToInt()}°",
                    if (abs(live.tiltDeg) < 3) "level" else if (live.tiltDeg > 0) "climbing" else "descending",
                    Modifier.weight(1f),
                )
                BigStat(
                    "LIGHT",
                    if (live.lightLux >= 0) "${live.lightLux.roundToInt()}" else "—",
                    if (live.lightLux >= 0) "lux · ${if (live.lightLux < 50) "night" else "daylight"}" else "no light sensor",
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat("ALTITUDE", if (live.gpsFix) "${live.altitudeM} m" else "—", Modifier.weight(1f))
                SmallStat("HARSH EVENTS", "${live.harshEvents}", Modifier.weight(1f))
                SmallStat(
                    "CORNERING",
                    String.format("%.2fg", abs(live.lateralG)),
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "HARDWARE ON THIS UNIT",
                fontSize = 11.sp, letterSpacing = 2.sp, color = Twende.Dim,
            )
            Spacer(Modifier.height(6.dp))

            LazyColumn(Modifier.weight(1f)) {
                items(inventory, key = { it.key }) { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(if (s.present) Twende.Cyan else Twende.Line),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                s.label, fontSize = 16.sp,
                                color = if (s.present) Twende.Ink else Twende.Dim,
                            )
                            if (s.detail.isNotBlank()) {
                                Text(s.detail, fontSize = 11.sp, color = Twende.Dim)
                            }
                        }
                        Text(
                            s.value, fontSize = 14.sp,
                            color = if (s.present) Twende.Cyan else Twende.Dim,
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Twende.Cyan)
                    .clickable { onRescan() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "RESCAN HARDWARE",
                    fontSize = 17.sp, fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp, color = Twende.Cosmic,
                )
            }
        }
    }
}

@Composable
private fun BigStat(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Twende.ButtonBg)
            .border(1.dp, Twende.Line, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Text(label, fontSize = 10.sp, letterSpacing = 2.sp, color = Twende.Dim)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 38.sp, fontWeight = FontWeight.Black, color = Twende.Cyan)
        Text(sub, fontSize = 11.sp, color = Twende.Dim)
    }
}

@Composable
private fun SmallStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Twende.ButtonBg)
            .padding(10.dp),
    ) {
        Text(label, fontSize = 9.sp, letterSpacing = 1.sp, color = Twende.Dim)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Twende.Ink)
    }
}
