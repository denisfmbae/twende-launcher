package co.nedlink.twende.data.obd

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import co.nedlink.twende.data.vehicle.TripComputer
import co.nedlink.twende.model.DtcReport
import co.nedlink.twende.model.Dtc
import co.nedlink.twende.model.SensorInfo
import co.nedlink.twende.model.SensorScan
import co.nedlink.twende.model.Telemetry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sin

/**
 * Bound service producing one Telemetry frame every 500ms while polling is on.
 * Two sources: a deterministic simulator (default — demo-safe on the bench)
 * and a live ELM327 dongle over Bluetooth SPP. No foreground notification, no
 * wake locks: when the launcher backgrounds, ObdRepository switches polling
 * off and this loop parks on a suspended flag — zero CPU.
 */
@AndroidEntryPoint
class ObdService : Service() {

    @Inject lateinit var trip: TripComputer

    inner class LocalBinder : Binder() {
        val telemetry: StateFlow<Telemetry> get() = this@ObdService.telemetry
        fun configure(simulated: Boolean, mac: String) = this@ObdService.configure(simulated, mac)
        fun setPolling(active: Boolean) { polling.value = active }
        fun demoDrive(on: Boolean) = this@ObdService.setDemoDriving(on)
        suspend fun scanDtcs(): DtcReport = this@ObdService.scanDtcs()
        suspend fun scanSensors(): SensorScan = this@ObdService.scanSensors()
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val telemetry = MutableStateFlow(Telemetry())
    private val polling = MutableStateFlow(true)

    private var simulated = true
    private var elmMac = ""
    private val elm = Elm327Client()
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder = binder.also { ensureLoop() }

    private fun configure(sim: Boolean, mac: String) {
        simulated = sim
        elmMac = mac
        if (sim) elm.close()
    }

    private fun ensureLoop() {
        if (loop?.isActive == true) return
        loop = scope.launch {
            var t = 0.0
            while (isActive) {
                if (!polling.value) { delay(250); continue }
                telemetry.value = if (simulated || elmMac.isBlank()) {
                    t += 0.5
                    simulate(t)
                } else {
                    readLive() ?: Telemetry(source = Telemetry.Source.ELM327)
                }
                trip.onFrame(telemetry.value)
                delay(500)
            }
        }
    }

    /**
     * Demo telemetry for the bench — but HONEST at standstill. With no dongle
     * connected the app can't know real speed, so it must not invent motion:
     * it sits at a realistic engine idle (≈780 rpm, 0 km/h), exactly like a
     * parked car with the engine on. Motion only appears when [demoDriving] is
     * explicitly turned on (a "demo drive" toggle) OR when a real ELM327 feeds
     * live data. This is what makes the cluster trustworthy: stopped means stopped.
     */
    private fun simulate(t: Double): Telemetry {
        if (!demoDriving) {
            // Parked, engine idling. Tiny rpm flutter so it looks alive, zero speed.
            val idleRpm = (780 + sin(t * 1.7) * 25).toInt()
            return Telemetry(
                rpm = idleRpm,
                speedKmh = 0,
                fuelPct = fuelStart,
                coolantC = 88,
                batteryV = 14.1f,
                throttlePct = 0,
                engineLoadPct = 16,
                source = Telemetry.Source.SIMULATOR,
            )
        }
        // Demo-drive mode: the old plausible idle→cruise cycle, for showing off.
        val cruise = (sin(t / 14.0) + 1) / 2
        val rpm = (900 + cruise * 2600 + sin(t) * 120).toInt()
        return Telemetry(
            rpm = rpm,
            speedKmh = (cruise * 96 + abs(sin(t / 3)) * 4).toInt(),
            fuelPct = (fuelStart - t / 40).toInt().coerceIn(5, 100),
            coolantC = (78 + cruise * 14).toInt(),
            batteryV = if (rpm > 300) (14.0 + sin(t / 5) * 0.3).toFloat() else 12.5f,
            throttlePct = (cruise * 70 + abs(sin(t / 2)) * 12).toInt().coerceIn(0, 100),
            engineLoadPct = (18 + cruise * 55).toInt().coerceIn(0, 100),
            source = Telemetry.Source.SIMULATOR,
        )
    }

    @Volatile private var demoDriving = false
    private val fuelStart = 68
    fun setDemoDriving(on: Boolean) { demoDriving = on }

    /** One-shot fault-code scan (OBD-II Mode 03). Runs on IO, never the UI thread. */
    private suspend fun scanDtcs(): DtcReport = withContext(Dispatchers.IO) {
        if (simulated || elmMac.isBlank()) {
            // Bench demo so the feature is visible without a car attached.
            DtcReport(
                scanned = true, milOn = true, simulated = true,
                codes = listOf(
                    Dtc("P0301", DtcCatalog.describe("P0301")),
                    Dtc("P0420", DtcCatalog.describe("P0420")),
                ),
            )
        } else runCatching {
            if (!elm.isConnected) {
                val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                elm.connect(adapter, elmMac)
            }
            elm.readDtcs()
        }.getOrElse { DtcReport(scanned = true) }
    }

    /** Probe which standard OBD-II sensors this car exposes. */
    private suspend fun scanSensors(): SensorScan = withContext(Dispatchers.IO) {
        if (simulated || elmMac.isBlank()) {
            // Bench demo: present the sensors the simulator "answers" so the UI is
            // populated without a dongle, clearly flagged as simulated.
            SensorScan(
                scanned = true, connected = false, simulated = true,
                sensors = listOf(
                    SensorInfo("010C","Engine RPM",true,"3532 rpm"),
                    SensorInfo("010D","Vehicle speed",true,"95 km/h"),
                    SensorInfo("0105","Coolant temperature",true,"91 °C"),
                    SensorInfo("0104","Calculated engine load",true,"63 %"),
                    SensorInfo("0111","Throttle position",true,"42 %"),
                    SensorInfo("012F","Fuel level",true,"67 %"),
                    SensorInfo("0142","Control module voltage",true,"14.1 V"),
                    SensorInfo("010F","Intake air temperature",false),
                    SensorInfo("0110","Mass air flow (MAF)",false),
                    SensorInfo("010B","Intake manifold pressure",false),
                    SensorInfo("010A","Fuel pressure",false),
                    SensorInfo("0106","Short-term fuel trim",false),
                ),
            )
        } else runCatching {
            if (!elm.isConnected) {
                val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
                elm.connect(adapter, elmMac)
            }
            SensorScan(scanned = true, connected = true, simulated = false,
                sensors = elm.scanSupportedPids())
        }.getOrElse { SensorScan(scanned = true, connected = false) }
    }

    private fun readLive(): Telemetry? = runCatching {
        if (!elm.isConnected) {
            val adapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            elm.connect(adapter, elmMac)
        }
        elm.readTelemetry()
    }.getOrNull()

    override fun onDestroy() {
        elm.close()
        scope.cancel()
        super.onDestroy()
    }
}
