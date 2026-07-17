package co.nedlink.twende.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import co.nedlink.twende.model.Dtc
import co.nedlink.twende.model.SensorInfo
import co.nedlink.twende.model.Dtc
import co.nedlink.twende.model.SensorInfoReport
import co.nedlink.twende.model.Telemetry
import java.util.UUID

/**
 * Minimal ELM327 client over Bluetooth SPP. Blocking I/O — always call from
 * Dispatchers.IO. PIDs used:
 *   010C RPM      = (A*256 + B) / 4
 *   010D Speed    = A (km/h)
 *   012F Fuel     = A * 100 / 255 (%)
 *   0105 Coolant  = A - 40 (°C)
 */
class Elm327Client {

    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var socket: BluetoothSocket? = null

    val isConnected: Boolean get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    fun connect(adapter: BluetoothAdapter, mac: String) {
        close()
        val device = adapter.getRemoteDevice(mac)
        adapter.cancelDiscovery()
        socket = device.createRfcommSocketToServiceRecord(sppUuid).also { it.connect() }
        // Standard ELM init: reset, echo off, linefeeds off, auto protocol
        listOf("ATZ", "ATE0", "ATL0", "ATSP0").forEach { command(it) }
    }

    fun readTelemetry(): Telemetry = Telemetry(
        rpm = pidBytes("010C", 2)?.let { (it[0] * 256 + it[1]) / 4 } ?: 0,
        speedKmh = pidBytes("010D", 1)?.get(0) ?: 0,
        fuelPct = pidBytes("012F", 1)?.let { it[0] * 100 / 255 } ?: 0,
        coolantC = pidBytes("0105", 1)?.let { it[0] - 40 } ?: 0,
        batteryV = pidBytes("0142", 2)?.let { (it[0] * 256 + it[1]) / 1000f } ?: 0f,
        throttlePct = pidBytes("0111", 1)?.let { it[0] * 100 / 255 } ?: 0,
        engineLoadPct = pidBytes("0104", 1)?.let { it[0] * 100 / 255 } ?: 0,
        source = Telemetry.Source.ELM327,
    )

    /**
     * Mode 03 — read stored diagnostic trouble codes, plus Mode 01 PID 01 for the
     * check-engine lamp state. This is the standard, universally supported way to
     * ask a car why its engine light is on.
     *
     * Caveat kept honest: this parses a single-frame response. A car with many
     * stored codes replies across multiple CAN frames, and some adapters format
     * those differently — expect the first few codes to be reliable and treat a
     * long list with suspicion.
     */
    fun readDtcs(): DtcReport {
        val milOn = pidBytes("0101", 1)?.let { (it[0] and 0x80) != 0 } ?: false
        val raw = command("03") ?: return DtcReport(scanned = true, milOn = milOn)
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val idx = hex.indexOf("43")
        if (idx < 0) return DtcReport(scanned = true, milOn = milOn)

        val payload = hex.substring(idx + 2)
        val codes = mutableListOf<Dtc>()
        var i = 0
        while (i + 4 <= payload.length) {
            val a = payload.substring(i, i + 2).toIntOrNull(16) ?: break
            val b = payload.substring(i + 2, i + 4).toIntOrNull(16) ?: break
            i += 4
            if (a == 0 && b == 0) continue          // padding
            val code = DtcCatalog.decode(a, b)
            codes += Dtc(code, DtcCatalog.describe(code))
        }
        return DtcReport(scanned = true, milOn = milOn, codes = codes.distinctBy { it.code })
    }

    /**
     * Mode 04 — clear codes. Deliberately NOT wired to a button in the UI: clearing
     * a code doesn't fix the fault, and it also wipes the emissions readiness
     * monitors, which can fail an inspection until the car has been driven through
     * a full drive cycle again. Left here as a deliberate, documented seam.
     */
    @Suppress("unused")
    fun clearDtcs(): Boolean = command("04")?.contains("44") == true

    /** Sends a PID query and extracts n data bytes after the 41xx echo. */
    /**
     * Ask the car which Mode-01 sensors it actually supports. OBD-II exposes this
     * via "support" PIDs: 0100 reports support for 01-20, 0120 for 21-40, 0140 for
     * 41-60 — each a 32-bit mask where a set bit = that PID is answerable. We read
     * those masks, then for the sensors Twende uses we also pull a live sample.
     *
     * This is the honest, real answer to "what can this car tell me" — it's the
     * car's own declared capability, not a guess. Body sensors (doors, seatbelt)
     * are absent by design: they aren't Mode-01 PIDs on a generic bus.
     */
    fun scanSupportedPids(): List<SensorInfo> {
        val supported = HashSet<Int>()
        listOf(0x00, 0x20, 0x40).forEach { base ->
            val cmd = "01%02X".format(base)
            val bytes = pidBytes(cmd, 4) ?: return@forEach
            var mask = 0L
            bytes.forEach { mask = (mask shl 8) or it.toLong() }
            for (bit in 0 until 32) {
                if ((mask shr (31 - bit)) and 1L == 1L) supported.add(base + bit + 1)
            }
        }
        return KNOWN.map { (pid, name) ->
            val num = pid.removePrefix("01").toInt(16)
            val ok = supported.contains(num)
            SensorInfo(
                pid = pid,
                name = name,
                supported = ok,
                sampleValue = if (ok) sampleFor(pid) else "",
            )
        }
    }

    private fun sampleFor(pid: String): String = runCatching {
        when (pid) {
            "010C" -> pidBytes(pid, 2)?.let { "${(it[0] * 256 + it[1]) / 4} rpm" }
            "010D" -> pidBytes(pid, 1)?.let { "${it[0]} km/h" }
            "0105" -> pidBytes(pid, 1)?.let { "${it[0] - 40} °C" }
            "012F" -> pidBytes(pid, 1)?.let { "${it[0] * 100 / 255} %" }
            "0111" -> pidBytes(pid, 1)?.let { "${it[0] * 100 / 255} %" }
            "0104" -> pidBytes(pid, 1)?.let { "${it[0] * 100 / 255} %" }
            "0142" -> pidBytes(pid, 2)?.let { "%.1f V".format((it[0] * 256 + it[1]) / 1000f) }
            "010F" -> pidBytes(pid, 1)?.let { "${it[0] - 40} °C" }
            "0110" -> pidBytes(pid, 2)?.let { "%.2f g/s".format((it[0] * 256 + it[1]) / 100f) }
            "010B" -> pidBytes(pid, 1)?.let { "${it[0]} kPa" }
            "010A" -> pidBytes(pid, 1)?.let { "${it[0] * 3} kPa" }
            "0106" -> pidBytes(pid, 1)?.let { "%+d %%".format((it[0] - 128) * 100 / 128) }
            else -> null
        } ?: ""
    }.getOrDefault("")

    private companion object {
        // The common Mode-01 sensors worth surfacing, in a sensible order.
        val KNOWN = listOf(
            "010C" to "Engine RPM",
            "010D" to "Vehicle speed",
            "0105" to "Coolant temperature",
            "010F" to "Intake air temperature",
            "0104" to "Calculated engine load",
            "0111" to "Throttle position",
            "012F" to "Fuel level",
            "0142" to "Control module voltage",
            "0110" to "Mass air flow (MAF)",
            "010B" to "Intake manifold pressure",
            "010A" to "Fuel pressure",
            "0106" to "Short-term fuel trim",
        )
    }

    private fun pidBytes(pid: String, n: Int): List<Int>? {
        val raw = command(pid) ?: return null
        val hex = raw.uppercase().replace(Regex("[^0-9A-F]"), "")
        val marker = "41" + pid.substring(2)
        val idx = hex.indexOf(marker)
        if (idx < 0 || hex.length < idx + marker.length + n * 2) return null
        return (0 until n).map { i ->
            hex.substring(idx + marker.length + i * 2, idx + marker.length + i * 2 + 2).toInt(16)
        }
    }

    /**
     * Writes cmd + CR, reads until the ELM '>' prompt — or until [READ_TIMEOUT_MS]
     * elapses. The timeout is a robustness/security guard: a dead or hostile dongle
     * that never sends '>' must not hang the reader thread forever. Reads are gated
     * on available() so a silent adapter can't block indefinitely either.
     */
    private fun command(cmd: String): String? {
        val s = socket ?: return null
        return runCatching {
            s.outputStream.write((cmd + "\r").toByteArray())
            s.outputStream.flush()
            val sb = StringBuilder()
            val buf = ByteArray(128)
            val deadline = System.currentTimeMillis() + READ_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline) {
                if (s.inputStream.available() > 0) {
                    val read = s.inputStream.read(buf)
                    if (read < 0) break
                    sb.append(String(buf, 0, read))
                    if (sb.contains('>')) break
                } else {
                    Thread.sleep(5)
                }
            }
            sb.toString()
        }.getOrNull()
    }

    private companion object {
        const val READ_TIMEOUT_MS = 2000L
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }
}
