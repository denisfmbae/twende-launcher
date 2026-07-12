package co.nedlink.twende.data.obd

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import co.nedlink.twende.model.Telemetry
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
import kotlin.math.abs
import kotlin.math.sin

/**
 * Bound service producing one Telemetry frame every 500ms while polling is on.
 * Two sources: a deterministic simulator (default — demo-safe on the bench)
 * and a live ELM327 dongle over Bluetooth SPP. No foreground notification, no
 * wake locks: when the launcher backgrounds, ObdRepository switches polling
 * off and this loop parks on a suspended flag — zero CPU.
 */
class ObdService : Service() {

    inner class LocalBinder : Binder() {
        val telemetry: StateFlow<Telemetry> get() = this@ObdService.telemetry
        fun configure(simulated: Boolean, mac: String) = this@ObdService.configure(simulated, mac)
        fun setPolling(active: Boolean) { polling.value = active }
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
                delay(500)
            }
        }
    }

    /** Smooth, plausible bench data: idle→cruise cycles with fuel slowly draining. */
    private fun simulate(t: Double): Telemetry {
        val cruise = (sin(t / 14.0) + 1) / 2                 // 0..1 driving cycle
        return Telemetry(
            rpm = (900 + cruise * 2600 + sin(t) * 120).toInt(),
            speedKmh = (cruise * 96 + abs(sin(t / 3)) * 4).toInt(),
            fuelPct = (68 - t / 40).toInt().coerceIn(5, 100),
            coolantC = (78 + cruise * 14).toInt(),
            source = Telemetry.Source.SIMULATOR,
        )
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
