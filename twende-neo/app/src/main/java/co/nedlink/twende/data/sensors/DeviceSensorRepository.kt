package co.nedlink.twende.data.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** One reading from a sensor the head unit itself owns. */
data class DeviceSensor(
    val key: String,
    val label: String,
    val value: String,
    val detail: String = "",
    val present: Boolean = true,
)

/** Live values derived from the unit's own hardware — no OBD dongle involved. */
data class DeviceTelemetry(
    val gpsSpeedKmh: Int = 0,
    val gpsFix: Boolean = false,
    val satellites: Int = 0,
    val altitudeM: Int = 0,
    val gForce: Float = 0f,          // total horizontal g
    val accelG: Float = 0f,          // fore/aft: + accelerating, - braking
    val lateralG: Float = 0f,        // cornering
    val tiltDeg: Float = 0f,         // pitch, for hills
    val lightLux: Float = -1f,       // -1 = no light sensor
    val harshEvents: Int = 0,
)

/**
 * The head unit is an Android tablet, so it carries real sensors of its own —
 * GPS, accelerometer, usually a light sensor, sometimes a gyroscope. Those work
 * with nothing plugged into the car, which makes them the honest answer to "show
 * me sensor data" on a vehicle with no OBD dongle attached: GPS gives true road
 * speed, the accelerometer gives braking/cornering g-force, and the light sensor
 * can drive automatic day/night switching.
 */
@Singleton
class DeviceSensorRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val sm: SensorManager? =
        runCatching { ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager }.getOrNull()

    private val lm: LocationManager? =
        runCatching { ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager }.getOrNull()

    /** What this particular unit actually has. Cheap, safe to call any time. */
    fun inventory(): List<DeviceSensor> {
        val out = mutableListOf<DeviceSensor>()
        fun add(type: Int, key: String, label: String, note: String) {
            val s = runCatching { sm?.getDefaultSensor(type) }.getOrNull()
            out += DeviceSensor(
                key = key,
                label = label,
                value = if (s != null) "Available" else "Not fitted",
                detail = if (s != null) (s.name.take(40).ifBlank { note }) else note,
                present = s != null,
            )
        }
        add(Sensor.TYPE_ACCELEROMETER, "accel", "Accelerometer", "g-force, braking, cornering")
        add(Sensor.TYPE_GYROSCOPE, "gyro", "Gyroscope", "rotation rate")
        add(Sensor.TYPE_MAGNETIC_FIELD, "mag", "Magnetometer", "compass heading")
        add(Sensor.TYPE_ROTATION_VECTOR, "rot", "Rotation vector", "fused orientation")
        add(Sensor.TYPE_LIGHT, "light", "Ambient light", "auto day/night")
        add(Sensor.TYPE_PRESSURE, "press", "Barometer", "altitude, weather")
        add(Sensor.TYPE_AMBIENT_TEMPERATURE, "temp", "Ambient temperature", "cabin temp")
        add(Sensor.TYPE_PROXIMITY, "prox", "Proximity", "near/far detection")

        val gps = runCatching {
            lm?.getProviders(true)?.contains(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)
        out += DeviceSensor(
            key = "gps",
            label = "GPS receiver",
            value = if (gps) "Available" else "Off or missing",
            detail = if (gps) "true road speed, altitude" else "enable location on the unit",
            present = gps,
        )
        return out
    }

    /** Everything the unit can measure, sampled live. */
    @SuppressLint("MissingPermission")
    fun telemetry(): Flow<DeviceTelemetry> = callbackFlow {
        var state = DeviceTelemetry()

        // Low-pass filtered gravity, so tilt and linear g can be separated.
        val gravity = FloatArray(3)
        var harsh = 0

        val sensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        val alpha = 0.8f
                        for (i in 0..2) {
                            gravity[i] = alpha * gravity[i] + (1 - alpha) * e.values[i]
                        }
                        val lx = e.values[0] - gravity[0]
                        val ly = e.values[1] - gravity[1]
                        val g = sqrt(lx * lx + ly * ly) / SensorManager.GRAVITY_EARTH
                        val fore = ly / SensorManager.GRAVITY_EARTH
                        val lat = lx / SensorManager.GRAVITY_EARTH
                        if (g > 0.45f) harsh++          // rough braking / swerve threshold
                        // pitch from gravity vector = how steep the road is
                        val pitch = Math.toDegrees(
                            kotlin.math.atan2(
                                gravity[1].toDouble(),
                                sqrt((gravity[0] * gravity[0] + gravity[2] * gravity[2]).toDouble()),
                            )
                        ).toFloat()
                        state = state.copy(
                            gForce = g,
                            accelG = fore,
                            lateralG = lat,
                            tiltDeg = if (abs(pitch) < 90f) pitch else 0f,
                            harshEvents = harsh,
                        )
                        trySend(state)
                    }
                    Sensor.TYPE_LIGHT -> {
                        state = state.copy(lightLux = e.values[0])
                        trySend(state)
                    }
                }
            }
        }

        val accel = runCatching { sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }.getOrNull()
        val light = runCatching { sm?.getDefaultSensor(Sensor.TYPE_LIGHT) }.getOrNull()
        runCatching {
            if (accel != null) sm?.registerListener(sensorListener, accel, SensorManager.SENSOR_DELAY_UI)
            if (light != null) sm?.registerListener(sensorListener, light, SensorManager.SENSOR_DELAY_NORMAL)
        }

        val locListener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                state = state.copy(
                    gpsSpeedKmh = (l.speed * 3.6f).roundToInt().coerceAtLeast(0),
                    gpsFix = true,
                    altitudeM = l.altitude.roundToInt(),
                    satellites = l.extras?.getInt("satellites", 0) ?: 0,
                )
                trySend(state)
            }
            @Deprecated("legacy API on Android 7 units")
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {
                state = state.copy(gpsFix = false)
                trySend(state)
            }
        }
        runCatching {
            lm?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, locListener)
        }

        trySend(state)
        awaitClose {
            runCatching { sm?.unregisterListener(sensorListener) }
            runCatching { lm?.removeUpdates(locListener) }
        }
    }.conflate()
}
