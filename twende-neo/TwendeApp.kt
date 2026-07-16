package co.nedlink.twende.data.obd

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import co.nedlink.twende.data.vehicle.TripComputer
import co.nedlink.twende.model.DtcReport
import co.nedlink.twende.model.Dtc
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
        suspend fun scanDtcs(): DtcReport = this@ObdService.scanDtcs()
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

    /** Smooth, plausible bench data: idle→cruise cycles with fuel slowly draining. */
    private fun simulate(t: Double): Telemetry {
        val cruise = (sin(t / 14.0) + 1) / 2                 // 0..1 driving cycle
        val rpm = (900 + cruise * 2600 + sin(t) * 120).toInt()
        return Telemetry(
            rpm = rpm,
            speedKmh = (cruise * 96 + abs(sin(t / 3)) * 4).toInt(),
            fuelPct = (68 - t / 40).toInt().coerceIn(5, 100),
            coolantC = (78 + cruise * 14).toInt(),
            batteryV = if (rpm > 300) (14.0 + sin(t / 5) * 0.3).toFloat() else 12.5f,
            throttlePct = (cruise * 70 + abs(sin(t / 2)) * 12).toInt().coerceIn(0, 100),
            engineLoadPct = (18 + cruise * 55).toInt().coerceIn(0, 100),
            source = Telemetry.Source.SIMULATOR,
        )
    }

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
