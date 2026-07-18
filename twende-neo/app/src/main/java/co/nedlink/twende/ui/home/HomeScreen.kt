package co.nedlink.twende.ui.home

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.nedlink.twende.model.Accessory
import co.nedlink.twende.model.AppEntry
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.ui.theme.neonStyle
import co.nedlink.twende.vm.LauncherViewModel
import co.nedlink.twende.vm.VehicleViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.app.Activity
import android.content.Intent
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

@Composable
fun HomeScreen(
    vehicle: VehicleViewModel = hiltViewModel(),
    launcher: LauncherViewModel = hiltViewModel(),
) {
    val telemetry by vehicle.telemetry.collectAsStateWithLifecycle()
    val heading by vehicle.headingDeg.collectAsStateWithLifecycle()
    val bt by vehicle.btState.collectAsStateWithLifecycle()
    val carLink by vehicle.carLink.collectAsStateWithLifecycle()
    val prefs by vehicle.prefs.collectAsStateWithLifecycle()
    val trip by vehicle.trip.collectAsStateWithLifecycle()
    val dtc by vehicle.dtc.collectAsStateWithLifecycle()
    val scanningDtc by vehicle.scanningDtc.collectAsStateWithLifecycle()
    val sensorScan by vehicle.sensorScan.collectAsStateWithLifecycle()
    val demoDriving by vehicle.demoDriving.collectAsStateWithLifecycle()
    val scanningSensors by vehicle.scanningSensors.collectAsStateWithLifecycle()
    var simpleMode by rememberSaveable { mutableStateOf(false) }
    var showSensors by rememberSaveable { mutableStateOf(false) }
    var screenOff by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity
    LaunchedEffect(screenOff) {
        activity?.window?.let { w ->
            val lp = w.attributes
            lp.screenBrightness =
                if (screenOff) 0.02f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            w.attributes = lp
        }
    }
    val pois by vehicle.pois.collectAsStateWithLifecycle()
    val searching by vehicle.searching.collectAsStateWithLifecycle()
    val apps by launcher.apps.collectAsStateWithLifecycle()
    val accessories by launcher.accessories.collectAsStateWithLifecycle()
    val nowPlaying by launcher.nowPlaying.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {

            TopStatusBar(
                bt = bt.hfpConnected || bt.a2dpConnected,
                btLabel = bt.deviceName?.let { n -> bt.batteryPct?.let { "$n · $it%" } ?: n } ?: "Not connected",
                carLink = carLink.connected,
                glow = prefs.glowIntensity,
                simpleMode = simpleMode,
                onToggleSimple = { simpleMode = !simpleMode },
            )

            Spacer(Modifier.height(10.dp))

            if (simpleMode) {
                SimpleMode(
                    speedKmh = telemetry.speedKmh,
                    fuelPct = telemetry.fuelPct,
                    apps = launcher.commuterApps.ifEmpty { apps.take(6) },
                    onLaunch = launcher::launch,
                    onSensors = { showSensors = true; vehicle.scanSensors() },
                    modifier = Modifier.weight(1f),
                )
            } else {

            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TelemetryCluster(
                    t = telemetry,
                    trip = trip,
                    metric = prefs.metricUnits,
                    glow = prefs.glowIntensity,
                    speedLimitKmh = prefs.speedLimitKmh,
                    tankLitres = prefs.tankLitres,
                    demoDriving = demoDriving,
                    onToggleDemo = vehicle::toggleDemoDriving,
                    modifier = Modifier.weight(0.30f),
                )
                Box(Modifier.weight(0.40f)) {
                    CarSimulationWidget(
                        speedKmh = telemetry.speedKmh,
                        heading = heading,
                        glow = prefs.glowIntensity,
                    )
                }
                Column(Modifier.weight(0.30f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ConnCard("BLUETOOTH", bt.deviceName ?: "Device not connected",
                        active = bt.hfpConnected || bt.a2dpConnected,
                        detail = listOfNotNull(
                            if (bt.hfpConnected) "Calls" else null,
                            if (bt.a2dpConnected) "Audio" else null,
                            bt.batteryPct?.let { "$it% batt" },
                        ).joinToString(" · ").ifEmpty { "—" })
                    ConnCard("CAR LINK 2.0", carLink.peer ?: "Device not connected",
                        active = carLink.connected, detail = if (carLink.connected) "Mirroring" else "Waiting")
                    PoiSearchBar(pois, searching, onOpen = vehicle::searchPois)
                }
            }

            Spacer(Modifier.height(8.dp))
            VitalsStrip(
                trip = trip,
                t = telemetry,
                dtc = dtc,
                scanning = scanningDtc,
                priceSet = prefs.fuelPriceKes > 0f,
                onScan = vehicle::scanDtcs,
            )
            if (dtc.codes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                DtcPanel(dtc)
            }

            if (nowPlaying.active) {
                Spacer(Modifier.height(8.dp))
                NowPlayingBar(
                    np = nowPlaying,
                    onPrevious = launcher::mediaPrevious,
                    onPlayPause = launcher::mediaPlayPause,
                    onNext = launcher::mediaNext,
                    onGrantAccess = launcher::grantMediaAccess,
                )
            }

            Spacer(Modifier.height(8.dp))
            QuickRail(onScreenOff = { screenOff = true })

            Spacer(Modifier.height(8.dp))
            BottomDock(
                apps = launcher.commuterApps.ifEmpty { apps.take(8) },
                accessories = accessories,
                onLaunch = launcher::launch,
                onAccessory = launcher::openAccessory,
                onSensors = { showSensors = true; vehicle.scanSensors() },
            )
            } // end else (full dashboard)
        }

        // Sensor scan overlay sits above everything.
        if (showSensors) {
            SensorScanPanel(
                scan = sensorScan,
                scanning = scanningSensors,
                onScan = vehicle::scanSensors,
                onClose = { showSensors = false },
            )
        }

        if (screenOff) {
            Box(
                Modifier.fillMaxSize().background(Color.Black)
                    .clickable { screenOff = false },
                contentAlignment = Alignment.Center,
            ) {
                Text("tap to wake", color = Color(0xFF1B2430), fontSize = 12.sp)
            }
        }
    }
}

/* ---------- top bar ---------- */

@Composable
private fun TopStatusBar(bt: Boolean, btLabel: String, carLink: Boolean, glow: Float, simpleMode: Boolean, onToggleSimple: () -> Unit) {
    val time by produceState(initialValue = LocalDateTime.now()) {
        while (true) { value = LocalDateTime.now(); delay(1000) }
    }
    val ctx = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            time.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = neonStyle(Twende.Cyan, 34, glow),
            modifier = Modifier.clickable {
                // Twende shows the unit's own system clock; if it's wrong, the fix
                // is the unit's date/time settings — one tap away from here.
                runCatching {
                    ctx.startActivity(
                        Intent(android.provider.Settings.ACTION_DATE_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            },
        )
        Spacer(Modifier.width(14.dp))
        Text(time.format(DateTimeFormatter.ofPattern("EEE d MMM")).uppercase(),
            color = Twende.Dim, fontSize = 12.sp, letterSpacing = 2.sp)
        Spacer(Modifier.weight(1f))
        StatusDot("BT", bt); Spacer(Modifier.width(12.dp))
        StatusDot("LINK", carLink); Spacer(Modifier.width(12.dp))
        StatusDot("REC", false) // recorder stub — wire to the unit's DVR broadcast when available
    }
}

@Composable
private fun StatusDot(label: String, active: Boolean) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape)
            .background(if (active) Twende.Cyan.copy(alpha = pulse) else Color(0xFF39424D)))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, letterSpacing = 1.5.sp,
            color = if (active) Twende.Cyan else Twende.Dim)
    }
}

@Composable
private fun ConnCard(title: String, name: String, active: Boolean, detail: String) {
    Column(Modifier.fillMaxWidth().glass(16).padding(12.dp)) {
        Text(title, fontSize = 9.sp, letterSpacing = 2.sp, color = if (active) Twende.Cyan else Twende.Dim)
        Text(name, fontSize = 13.sp, color = Color(0xFFE8ECF1), maxLines = 1)
        Text(detail, fontSize = 10.sp, color = Twende.Dim, maxLines = 1)
    }
}

/* ---------- bottom dock: APPS | ACCESSORIES, sharing one carousel ---------- */

@Composable
private fun BottomDock(
    apps: List<AppEntry>,
    accessories: List<Accessory>,
    onLaunch: (String) -> Unit,
    onAccessory: (String) -> Unit,
    onSensors: () -> Unit = {},
) {
    var tab by remember { mutableIntStateOf(0) }
    val state = rememberLazyListState()

    Column {
        Row(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
            DockTab("COMMUTER APPS", tab == 0) { tab = 0 }
            Spacer(Modifier.width(18.dp))
            DockTab("ACCESSORIES", tab == 1) { tab = 1 }
        }
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (tab == 0) {
                itemsIndexed(apps, key = { i, a -> "${a.pkg}#$i" }) { index, app ->
                    DockIcon(app, index, state, onLaunch)
                }
            } else {
                item {
                    AccessoryTile("SCAN", "Sensors", available = true, onClick = onSensors)
                }
                items(accessories, key = { it.id }) { acc ->
                    AccessoryIcon(acc, onAccessory)
                }
            }
        }
    }
}

@Composable
private fun DockTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 9.sp,
        letterSpacing = 3.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Twende.Cyan else Twende.Dim,
        modifier = Modifier.clickable { onClick() },
    )
}

/**
 * An accessory tile. Unavailable ones (no Files app, no vendor radio on this unit)
 * render dimmed and inert rather than throwing ActivityNotFoundException mid-drive.
 */
@Composable
private fun AccessoryIcon(acc: Accessory, onOpen: (String) -> Unit) {
    val tint = if (acc.available) Twende.Cyan else Color(0xFF3A4450)
    Column(
        Modifier
            .width(92.dp)
            .glass(18)
            .clickable(enabled = acc.available) { onOpen(acc.id) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (acc.available) Color(0x1A00E5FF) else Color(0x0DFFFFFF))
                .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(acc.glyph, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (acc.available) acc.label else "${acc.label} —",
            fontSize = 10.sp,
            color = if (acc.available) Color(0xFFCBD4DC) else Color(0xFF5A6472),
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DockIcon(app: AppEntry, index: Int, state: androidx.compose.foundation.lazy.LazyListState, onLaunch: (String) -> Unit) {
    Column(
        Modifier
            .width(92.dp)
            .graphicsLayer {
                val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return@graphicsLayer
                val viewportCenter = (state.layoutInfo.viewportStartOffset + state.layoutInfo.viewportEndOffset) / 2f
                val d = (((info.offset + info.size / 2f) - viewportCenter) / viewportCenter).coerceIn(-1f, 1f)
                val s = 1f - 0.14f * abs(d)
                scaleX = s; scaleY = s
                rotationY = -14f * d
                alpha = 1f - 0.30f * abs(d)
            }
            .glass(18)
            .clickable { onLaunch(app.pkg) }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon: Bitmap? = app.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(), contentDescription = app.label,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Twende.Cyan.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            )
        } else {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Twende.Panel))
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, fontSize = 10.sp, color = Color(0xFFCBD4DC), maxLines = 1, textAlign = TextAlign.Center)
    }
}

/* A generic big dock tile (used for the Sensors scanner entry). */
@Composable
private fun AccessoryTile(glyph: String, label: String, available: Boolean, onClick: () -> Unit) {
    val tint = if (available) Twende.Cyan else Color(0xFF3A4450)
    Column(
        Modifier
            .width(96.dp)
            .glass(18)
            .clickable(enabled = available) { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                .background(Color(0x1A00E5FF))
                .border(1.dp, tint.copy(alpha = 0.55f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tint) }
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFFCBD4DC), maxLines = 1)
    }
}

/* ---------- SIMPLE (driving) MODE: few, huge tiles ---------- */
@Composable
private fun SimpleMode(
    speedKmh: Int,
    fuelPct: Int,
    apps: List<AppEntry>,
    onLaunch: (String) -> Unit,
    onSensors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Huge speed + fuel banner
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f).fillMaxSize().glass(20), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$speedKmh", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Twende.Cyan)
                    Text("km/h", fontSize = 14.sp, color = Twende.Dim)
                }
            }
            Box(Modifier.weight(1f).fillMaxSize().glass(20), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$fuelPct%", fontSize = 56.sp, fontWeight = FontWeight.Black,
                        color = fuelColor(fuelPct))
                    Text("FUEL", fontSize = 14.sp, color = Twende.Dim)
                }
            }
        }
        // Big app buttons — 3 across, tall, easy to hit
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(apps.take(6), key = { it.pkg }) { app ->
                Box(
                    Modifier.width(150.dp).fillMaxSize().glass(20)
                        .clickable { onLaunch(app.pkg) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(app.label, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = Color(0xFFE8ECF1), maxLines = 2, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp))
                }
            }
            item {
                Box(
                    Modifier.width(150.dp).fillMaxSize().glass(20).clickable { onSensors() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("SCAN\nSENSORS", fontSize = 18.sp, fontWeight = FontWeight.Black,
                        color = Twende.Cyan, textAlign = TextAlign.Center)
                }
            }
        }
    }
}


/* ---------- quick actions: one tap, glove-sized ---------- */

@Composable
private fun QuickRail(onScreenOff: () -> Unit) {
    val ctx = LocalContext.current
    fun open(action: String) {
        runCatching {
            ctx.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickButton("BT", "Bluetooth", Modifier.weight(1f)) {
            open(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
        }
        QuickButton("WIFI", "Wi-Fi", Modifier.weight(1f)) {
            open(android.provider.Settings.ACTION_WIFI_SETTINGS)
        }
        QuickButton("♪", "Music", Modifier.weight(1f)) {
            runCatching {
                ctx.startActivity(
                    Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        QuickButton("⏻", "Screen off", Modifier.weight(1f), accent = true) { onScreenOff() }
    }
}

@Composable
private fun QuickButton(
    glyph: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .height(64.dp)
            .glass(16)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(glyph, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = if (accent) Twende.Magenta else Twende.Cyan)
        Text(label, fontSize = 10.sp, color = Twende.Dim)
    }
}
