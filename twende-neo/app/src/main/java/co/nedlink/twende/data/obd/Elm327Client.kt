package co.nedlink.twende.data.obd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
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
        source = Telemetry.Source.ELM327,
    )

    /** Sends a PID query and extracts n data bytes after the 41xx echo. */
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

    /** Writes cmd + CR, reads until the ELM '>' prompt. */
    private fun command(cmd: String): String? {
        val s = socket ?: return null
        return runCatching {
            s.outputStream.write((cmd + "\r").toByteArray())
            s.outputStream.flush()
            val sb = StringBuilder()
            val buf = ByteArray(128)
            while (true) {
                val read = s.inputStream.read(buf)
                if (read < 0) break
                sb.append(String(buf, 0, read))
                if (sb.contains('>')) break
            }
            sb.toString()
        }.getOrNull()
    }

    fun close() {
        runCatching { socket?.close() }
        socket = null
    }
}
