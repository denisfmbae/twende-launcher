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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import co.nedlink.twende.ui.common.BigHomeButton
import co.nedlink.twende.ui.common.BigNavButton
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Brush
import kotlin.math.roundToInt
import co.nedlink.twende.ui.video.VideoPanel
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
    onOpenApps: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMusic: () -> Unit = {},
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
    val deviceLive by vehicle.deviceTelemetry.collectAsStateWithLifecycle()
    val scanningSensors by vehicle.scanningSensors.collectAsStateWithLifecycle()
    var simpleMode by rememberSaveable { mutableStateOf(false) }
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
    val commuter by launcher.commuterApps.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        // Content and the volume rail are SIBLINGS in a Row, not stacked layers.
        // The rail used to be a floating overlay, which is exactly why it landed
        // on top of the transport buttons.
        Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)) {
            Column(Modifier.weight(1f)) {

                TopStatusBar(
                    bt = bt.hfpConnected || bt.a2dpConnected,
                    btLabel = bt.deviceName?.let { n -> bt.batteryPct?.let { "$n · $it%" } ?: n } ?: "Not connected",
                    carLink = carLink.connected,
                    glow = Twende.glowLevel,
                    simpleMode = simpleMode,
                    onToggleSimple = { simpleMode = !simpleMode },
                )

                Spacer(Modifier.height(8.dp))

                if (simpleMode) {
                    SimpleMode(
                        speedKmh = telemetry.speedKmh,
                        fuelPct = telemetry.fuelPct,
                        apps = commuter.ifEmpty { apps.take(6) },
                        onLaunch = launcher::launch,
                        onSensors = {},
                        modifier = Modifier.weight(1f),
                    )
                } else {

                // Media/active-app panel on the LEFT, everything else on the right.
                Row(Modifier.weight(1f).fillMaxWidth()) {

                    // Left, compressed: the video stage with its controls beneath.
                    VideoPanel(
                        modifier = Modifier.weight(0.34f).fillMaxHeight(),
                    )

                    Spacer(Modifier.width(12.dp))

                    // Right: the app surface, given the whole remaining view area.
                    Column(Modifier.weight(0.66f).fillMaxHeight()) {

                        SpeedStrip(live = deviceLive, modifier = Modifier.fillMaxWidth())

                        Spacer(Modifier.height(10.dp))

                        AppSurface(
                            apps = commuter.ifEmpty { apps },
                            onLaunch = launcher::launch,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )

                        Spacer(Modifier.height(10.dp))
                        QuickRail(onScreenOff = { screenOff = true })

                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BigNavButton("\u25a6", "APPS", widthDp = 150, onClick = onOpenApps)
                            BigHomeButton(onClick = { simpleMode = false })
                            BigNavButton("\u2699", "SETUP", widthDp = 150, onClick = onOpenSettings)
                        }
                    }
                }

                } // end else (full dashboard)
            }

            Spacer(Modifier.width(6.dp))
            VolumeRail(Modifier.fillMaxHeight().width(62.dp))
        }

        // Sensor scan overlay sits above everything.
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
        Spacer(Modifier.width(14.dp))
        ModeChip(if (simpleMode) "FULL" else "SIMPLE") { onToggleSimple() }
        Spacer(Modifier.width(8.dp))
        ModeChip(if (Twende.isLight) "NIGHT" else "DAY") { Twende.isLight = !Twende.isLight }
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
        Text(name, fontSize = 13.sp, color = Twende.Ink, maxLines = 1)
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
    val state = rememberLazyListState()

    Column {
        DockTab("COMMUTER APPS", true) {}
        Spacer(Modifier.height(6.dp))
        LazyRow(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(apps, key = { i, a -> "${a.pkg}#$i" }) { index, app ->
                DockIcon(app, index, state, onLaunch)
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
            color = if (acc.available) Twende.Ink else Color(0xFF5A6472),
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
        Text(app.label, fontSize = 10.sp, color = Twende.Ink, maxLines = 1, textAlign = TextAlign.Center)
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
        Text(label, fontSize = 11.sp, color = Twende.Ink, maxLines = 1)
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
                        color = Twende.Ink, maxLines = 2, textAlign = TextAlign.Center,
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
        QuickButton("▤", "Files", Modifier.weight(1f)) {
            // The generic "music app" selector resolves to nothing on many clone
            // units — a dead button is worse than none. Files instead, by request.
            val pm = ctx.packageManager
            val candidates = listOf(
                "com.android.documentsui", "com.google.android.documentsui",
                "com.android.fileexplorer", "com.mediatek.filemanager",
                "com.estrongs.android.pop", "com.rhmsoft.fm",
            )
            val launch = candidates.firstNotNullOfOrNull { pm.getLaunchIntentForPackage(it) }
            runCatching {
                ctx.startActivity(
                    (launch ?: Intent(Intent.ACTION_GET_CONTENT).setType("*/*"))
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


@Composable
private fun ModeChip(label: String, onClick: () -> Unit) {
    Text(
        label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
        color = Twende.Cyan,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Twende.ButtonBg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/* ---------- big centre speedometer: the home hero ---------- */

@Composable
private fun VolumeRail(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val audio = remember {
        runCatching {
            ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        }.getOrNull()
    }
    val maxVol = remember { (audio?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1) }
    var level by remember {
        mutableFloatStateOf(
            (audio?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0).toFloat() / maxVol
        )
    }
    fun apply(frac: Float) {
        val f = frac.coerceIn(0f, 1f)
        level = f
        runCatching {
            audio?.setStreamVolume(
                android.media.AudioManager.STREAM_MUSIC,
                (f * maxVol).roundToInt(), 0,
            )
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("VOL", fontSize = 9.sp, letterSpacing = 2.sp, color = Twende.Dim)
        Spacer(Modifier.height(6.dp))
        // One continuous track: drag anywhere along it, or tap a point to jump there.
        Box(
            Modifier
                .weight(1f)
                .width(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Twende.ButtonBg)
                .border(1.dp, Twende.Line, RoundedCornerShape(23.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        apply(1f - change.position.y / size.height.toFloat())
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { o -> apply(1f - o.y / size.height.toFloat()) }
                },
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(level.coerceIn(0.001f, 1f))
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Twende.Cyan.copy(alpha = 0.85f), Twende.Cyan.copy(alpha = 0.35f))
                        )
                    ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("${(level * 100).roundToInt()}%", fontSize = 11.sp, color = Twende.Ink)
        Spacer(Modifier.height(8.dp))
        VolButton("\u00d7") {
            runCatching {
                audio?.adjustStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.ADJUST_TOGGLE_MUTE, 0,
                )
            }
            apply(if (level > 0f) 0f else 0.4f)
        }
    }
}


@Composable
private fun VolButton(glyph: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(Twende.ButtonBg)
            .border(1.5.dp, Twende.Cyan.copy(alpha = 0.55f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Twende.Cyan)
    }
}

/* ---------- GPS speed: real road speed from the unit's own receiver ---------- */



/* ---------- the app surface: fills the main view area ---------- */

@Composable
private fun AppSurface(
    apps: List<AppEntry>,
    onLaunch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Twende.ButtonBg)
            .border(1.dp, Twende.Line, RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text("APPS", fontSize = 10.sp, letterSpacing = 3.sp, color = Twende.Dim)
        Spacer(Modifier.height(8.dp))
        // A launcher cannot draw another app inside itself — Android blocks one
        // app rendering another's surface — so a tap hands the whole screen over
        // to that app instead. Tiles are sized to be hit without aiming.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(apps, key = { i, a -> "${a.pkg}#$i" }) { _, app ->
                Column(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Twende.Panel)
                        .clickable { onLaunch(app.pkg) }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val icon = app.icon
                    if (icon != null) {
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(13.dp)),
                        )
                    } else {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(13.dp))
                                .background(Twende.ButtonBg),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        app.label, fontSize = 13.sp, color = Twende.Ink,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}


/* ---------- GPS speed strip: true road speed, no dongle required ---------- */

@Composable
private fun SpeedStrip(
    live: co.nedlink.twende.data.sensors.DeviceTelemetry,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Twende.ButtonBg)
            .border(1.dp, Twende.Line, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (live.gpsFix) "${live.gpsSpeedKmh}" else "--",
            fontSize = 46.sp, fontWeight = FontWeight.Black, color = Twende.Cyan,
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text("km/h", fontSize = 13.sp, letterSpacing = 2.sp, color = Twende.Dim)
            Text(
                if (live.gpsFix) "GPS" else "no fix",
                fontSize = 10.sp, letterSpacing = 1.sp,
                color = if (live.gpsFix) Twende.Cyan else Twende.Dim,
            )
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text("G-FORCE", fontSize = 9.sp, letterSpacing = 2.sp, color = Twende.Dim)
            Text(
                String.format("%.2f", live.gForce),
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Twende.Ink,
            )
        }
    }
}
