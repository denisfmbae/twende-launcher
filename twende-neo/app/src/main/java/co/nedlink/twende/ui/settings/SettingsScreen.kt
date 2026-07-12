package co.nedlink.twende.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.vm.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val p by vm.prefs.collectAsStateWithLifecycle()

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

            Column(Modifier.fillMaxWidth().glass(16).padding(14.dp)) {
                Text("Neon glow intensity", fontSize = 13.sp, color = Twende.Cyan)
                Slider(
                    value = p.glowIntensity, onValueChange = vm::setGlow,
                    valueRange = 0.2f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Twende.Cyan, activeTrackColor = Twende.Cyan),
                )
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
