package co.nedlink.twende.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.vm.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel(), onHome: () -> Unit = {}) {
    val p by vm.prefs.collectAsStateWithLifecycle()
    val launcher: co.nedlink.twende.vm.LauncherViewModel = hiltViewModel()
    val installed by launcher.apps.collectAsStateWithLifecycle()
    val pinned = p.commuterCsv.split(',').filter { it.isNotBlank() }

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("VEHICLE SETUP", fontSize = 10.sp, letterSpacing = 3.sp, color = Twende.Dim)

            SwitchRow("Metric units (km/h, °C)", p.metricUnits, vm::setMetric)
            SwitchRow("Simulate OBD telemetry", p.obdSimulated, vm::setSimulated)

            FieldRow(
                label = "ELM327 adapter MAC (pair in system BT first)",
                value = p.elmMac, enabled = !p.obdSimulated,
                placeholder = "00:1D:A5:68:98:8B", onChange = vm::setElmMac,
            )
            FieldRow(
                label = "Google Places API key (blank = offline suggestions)",
                value = p.placesKey, enabled = true,
                placeholder = "AIza…", onChange = vm::setPlacesKey,
            )

            Text("FUEL & LIMITS", fontSize = 10.sp, letterSpacing = 3.sp, color = Twende.Dim)

            NumberFieldRow(
                label = "Fuel tank size (litres) — drives range-to-empty and trip cost",
                initial = if (p.tankLitres > 0f) p.tankLitres.toInt().toString() else "",
                placeholder = "45",
                onCommit = { vm.setTankLitres(it.toFloatOrNull() ?: 0f) },
            )
            NumberFieldRow(
                label = "Fuel price (KES per litre) — EPRA revises this monthly",
                initial = if (p.fuelPriceKes > 0f) p.fuelPriceKes.toInt().toString() else "",
                placeholder = "today's pump price",
                onCommit = { vm.setFuelPrice(it.toFloatOrNull() ?: 0f) },
            )
            NumberFieldRow(
                label = "Overspeed alert (km/h) — 0 turns it off, 80 = PSV governor limit",
                initial = if (p.speedLimitKmh > 0) p.speedLimitKmh.toString() else "",
                placeholder = "80",
                onCommit = { vm.setSpeedLimit(it.toIntOrNull() ?: 0) },
            )

            Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
                Text("Neon glow intensity", fontSize = 13.sp, color = Twende.Cyan)
                Slider(
                    value = p.glowIntensity,
                    onValueChange = { Twende.glowLevel = it; vm.setGlow(it) },
                    valueRange = 0.2f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Twende.Cyan, activeTrackColor = Twende.Cyan),
                )
                Text(
                    "Lights the halo around every card and app tile. It updates live as you drag.",
                    fontSize = 11.sp, color = Twende.Dim,
                )
            }

            // ---- accent colour picker ----
            Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
                Text("Accent colour", fontSize = 13.sp, color = Twende.Cyan)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Twende.palette.forEachIndexed { i, pair ->
                        val selected = i == p.accentIdx
                        Box(
                            Modifier
                                .size(if (selected) 46.dp else 40.dp)
                                .clip(CircleShape)
                                .background(if (Twende.isLight) pair.second else pair.first)
                                .border(
                                    if (selected) 3.dp else 1.dp,
                                    if (selected) Twende.Ink else Twende.Line,
                                    CircleShape,
                                )
                                .clickable { vm.setAccent(i) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Chosen: ${Twende.accentNames.getOrElse(p.accentIdx) { "Cyan" }} — recolours the whole cockpit.",
                    fontSize = 11.sp, color = Twende.Dim,
                )
            }

            // ---- commuter dock customisation ----
            Text("HOME DOCK APPS", fontSize = 10.sp, letterSpacing = 3.sp, color = Twende.Dim)
            Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
                Text(
                    if (pinned.isEmpty())
                        "Tap apps to pin them to the home dock (up to 8). Empty = automatic."
                    else "${pinned.size} pinned · tap to add or remove",
                    fontSize = 12.sp, color = Twende.Cyan,
                )
                Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    installed.forEach { app ->
                        val on = app.pkg in pinned
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) Twende.Cyan.copy(alpha = 0.18f) else Twende.ButtonBg)
                                .border(1.dp, if (on) Twende.Cyan else Twende.Line, RoundedCornerShape(20.dp))
                                .clickable {
                                    val next = pinned.toMutableList()
                                    if (on) next.remove(app.pkg)
                                    else if (next.size < 8) next.add(app.pkg)
                                    vm.setCommuterCsv(next.joinToString(","))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (on) "\u2713 " else "", fontSize = 12.sp, color = Twende.Cyan)
                            Text(app.label, fontSize = 13.sp, color = Twende.Ink, maxLines = 1)
                        }
                    }
                }
                if (pinned.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "CLEAR — back to automatic",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Twende.Magenta,
                        modifier = Modifier.clickable { vm.setCommuterCsv("") },
                    )
                }
            }

            Text(
                "Twende Launcher · no ad SDKs, no analytics, no background services. " +
                "The only network call is the Places search you configure above.",
                fontSize = 11.sp, color = Twende.Dim, lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().glass(16).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 13.sp, color = Twende.Cyan, modifier = Modifier.weight(1f))
        Switch(
            checked = value, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Twende.Cyan, checkedThumbColor = Twende.Cosmic),
        )
    }
}

@Composable
private fun NumberFieldRow(label: String, initial: String, placeholder: String, onCommit: (String) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial) }
    Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
        Text(label, fontSize = 11.sp, color = Twende.Dim)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; onCommit(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text(placeholder, color = Twende.Dim) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Twende.Cyan,
                unfocusedBorderColor = Twende.Line,
                focusedTextColor = Twende.Cyan,
                unfocusedTextColor = Twende.Cyan,
            ),
        )
    }
}

@Composable
private fun FieldRow(label: String, value: String, enabled: Boolean, placeholder: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
        Text(label, fontSize = 11.sp, color = Twende.Dim)
        OutlinedTextField(
            value = value, onValueChange = onChange, enabled = enabled, singleLine = true,
            placeholder = { Text(placeholder, color = Twende.Dim) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Twende.Cyan,
                unfocusedBorderColor = Twende.Line,
                focusedTextColor = Twende.Cyan,
                unfocusedTextColor = Twende.Cyan,
            ),
        )
    }
}
