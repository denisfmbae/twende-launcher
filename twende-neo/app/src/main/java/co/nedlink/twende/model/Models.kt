package co.nedlink.twende.model

import android.graphics.Bitmap

data class Telemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val fuelPct: Int = 0,
    val coolantC: Int = 0,
    val source: Source = Source.SIMULATOR,
) {
    enum class Source { SIMULATOR, ELM327, GPS_ONLY }
}

data class BtState(
    val deviceName: String? = null,
    val hfpConnected: Boolean = false,
    val a2dpConnected: Boolean = false,
    val batteryPct: Int? = null,
)

data class CarLinkState(val connected: Boolean = false, val peer: String? = null)

data class AppEntry(val label: String, val pkg: String, val icon: Bitmap?)

data class Poi(val name: String, val address: String, val category: String, val offline: Boolean = false)

data class Prefs(
    val metricUnits: Boolean = true,
    val obdSimulated: Boolean = true,
    val elmMac: String = "",
    val placesKey: String = "",
    val glowIntensity: Float = 1f,
)
