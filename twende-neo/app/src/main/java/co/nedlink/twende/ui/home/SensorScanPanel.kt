package co.nedlink.twende.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.SensorScan
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass

private val OK_GREEN = Color(0xFF00E676)

/**
 * Sensor scanner overlay. Answers "what can this car actually tell me" by asking
 * the ECU which standard OBD-II PIDs it supports, then listing them with a live
 * sample for the supported ones. Big buttons, big rows — usable at a glance.
 */
@Composable
fun SensorScanPanel(
    scan: SensorScan,
    scanning: Boolean,
    onScan: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxSize()
            .background(Twende.Cosmic.copy(alpha = 0.96f))
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CAR SENSORS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Twende.Cyan)
                    Text(
                        when {
                            !scan.scanned -> "Tap SCAN to ask the car what it supports"
                            scan.simulated -> "Simulator — connect a dongle for real results"
                            scan.connected -> "${scan.supportedCount} sensors reported by the car"
                            else -> "No dongle connected"
                        },
                        fontSize = 12.sp, color = Twende.Dim,
                    )
                }
                // Big close button
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Color(0x1AFFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 22.sp, color = Twende.Cyan) }
            }

            Box(Modifier.height(14.dp))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(scan.sensors, key = { it.pid }) { s ->
                    Row(
                        Modifier.fillMaxWidth().glass(14).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(12.dp).clip(CircleShape)
                                .background(if (s.supported) OK_GREEN else Color(0xFF3A4450)),
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                color = if (s.supported) Color(0xFFE8ECF1) else Twende.Dim)
                            Text(s.pid, fontSize = 10.sp, color = Twende.Dim)
                        }
                        Text(
                            if (s.supported) (s.sampleValue.ifBlank { "supported" }) else "not available",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            color = if (s.supported) OK_GREEN else Twende.Dim,
                        )
                    }
                }
                if (scan.sensors.isEmpty()) {
                    items(listOf(0)) {
                        Text(
                            "No scan yet.",
                            fontSize = 14.sp, color = Twende.Dim,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                }
            }

            Box(Modifier.height(12.dp))

            // Big full-width scan button
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (scanning) Color(0x2200E5FF) else Twende.Cyan)
                    .clickable(enabled = !scanning) { onScan() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (scanning) "SCANNING…" else "SCAN CAR",
                    fontSize = 20.sp, fontWeight = FontWeight.Black,
                    color = if (scanning) Twende.Cyan else Twende.Cosmic,
                )
            }
        }
    }
}
