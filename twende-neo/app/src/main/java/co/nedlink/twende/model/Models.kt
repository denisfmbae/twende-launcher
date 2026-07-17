package co.nedlink.twende.model

import android.graphics.Bitmap

data class Telemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val fuelPct: Int = 0,
    val coolantC: Int = 0,
    // All standard OBD-II PIDs a generic ELM327 can read:
    val batteryV: Float = 0f,      // 0142 control module voltage
    val throttlePct: Int = 0,      // 0111 throttle position
    val engineLoadPct: Int = 0,    // 0104 calculated engine load
    val source: Source = Source.SIMULATOR,
) {
    enum class Source { SIMULATOR, ELM327, GPS_ONLY }

    /** Engine running but stationary. */
    val idling: Boolean get() = speedKmh < 1 && rpm > 300

    /** <12.4V at rest = weak battery. <13.2V while running = alternator not charging. */
    val batteryWarning: Boolean
        get() = batteryV > 1f && (if (rpm > 300) batteryV < 13.2f else batteryV < 12.4f)
}

/** A single diagnostic trouble code read from the car (OBD-II Mode 03). */
data class Dtc(val code: String, val desc: String)

data class DtcReport(
    val scanned: Boolean = false,
    val milOn: Boolean = false,          // is the check-engine lamp commanded on
    val codes: List<Dtc> = emptyList(),
    val simulated: Boolean = false,
)

/** Derived trip figures. Nothing here is read from the car — it's integrated from real telemetry. */
data class TripStats(
    val distanceKm: Float = 0f,
    val litresUsed: Float = 0f,
    val costKes: Float = 0f,
    val idleSeconds: Float = 0f,
    val idleCostKes: Float = 0f,
    val harshEvents: Int = 0,
    val ecoScore: Int = 100,
    val consumptionL100: Float = 0f,
    val rangeKm: Int = 0,
)

data class BtState(
    val deviceName: String? = null,
    val hfpConnected: Boolean = false,
    val a2dpConnected: Boolean = false,
    val batteryPct: Int? = null,
)

data class CarLinkState(val connected: Boolean = false, val peer: String? = null)

data class AppEntry(val label: String, val pkg: String, val icon: Bitmap?)

/** What's coming out of the speakers, whoever is playing it. */
data class NowPlaying(
    val active: Boolean = false,
    val playing: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val appLabel: String = "",
    val art: Bitmap? = null,
    /** False until the user grants notification access; controls still work without it. */
    val hasMetadataAccess: Boolean = false,
)

/** A system/accessory tile. [available] is false when nothing on this unit can handle it. */
data class Accessory(
    val id: String,
    val label: String,
    val glyph: String,
    val available: Boolean = true,
)

data class Poi(val name: String, val address: String, val category: String, val offline: Boolean = false)

data class Prefs(
    val metricUnits: Boolean = true,
    val obdSimulated: Boolean = true,
    val elmMac: String = "",
    val placesKey: String = "",
    val glowIntensity: Float = 1f,
    val tankLitres: Float = 45f,       // your tank size — drives range & cost
    val fuelPriceKes: Float = 0f,      // 0 = unset; EPRA revises pump prices monthly
    val speedLimitKmh: Int = 0,        // 0 = off. 80 = Kenyan PSV governor limit
)

enum class Door { FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT, HOOD, TRUNK }

/** Doors, hood and boot. Note the [Source] — see CarBodyRepository for why door
 *  status is simulated on generic hardware rather than read like OBD telemetry. */
data class BodyStatus(
    val frontLeft: Boolean = false,
    val frontRight: Boolean = false,
    val rearLeft: Boolean = false,
    val rearRight: Boolean = false,
    val hood: Boolean = false,
    val trunk: Boolean = false,
    val source: Source = Source.SIMULATOR,
) {
    enum class Source { SIMULATOR, MANUAL, VEHICLE }

    val openCount: Int
        get() = listOf(frontLeft, frontRight, rearLeft, rearRight, hood, trunk).count { it }
    val anyOpen: Boolean get() = openCount > 0

    fun withDoor(door: Door, open: Boolean, src: Source): BodyStatus = when (door) {
        Door.FRONT_LEFT -> copy(frontLeft = open, source = src)
        Door.FRONT_RIGHT -> copy(frontRight = open, source = src)
        Door.REAR_LEFT -> copy(rearLeft = open, source = src)
        Door.REAR_RIGHT -> copy(rearRight = open, source = src)
        Door.HOOD -> copy(hood = open, source = src)
        Door.TRUNK -> copy(trunk = open, source = src)
    }
}

/** One probed OBD-II sensor and whether the car answers it. */
data class SensorInfo(
    val pid: String,
    val name: String,
    val supported: Boolean,
    val sampleValue: String = "",
)

/** Result of scanning the car for which standard sensors it exposes. */
data class SensorScan(
    val scanned: Boolean = false,
    val connected: Boolean = false,
    val simulated: Boolean = false,
    val sensors: List<SensorInfo> = emptyList(),
) {
    val supportedCount: Int get() = sensors.count { it.supported }
}
