package co.nedlink.twende.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Compass heading (deg 0..360, magnetic) + GPS location/speed as cold flows. */
@Singleton
class HeadingProvider @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val fused: FusedLocationProviderClient,
) {

    /** Rotation-vector sensor → azimuth. SENSOR_DELAY_UI ≈ 60ms, cheap and smooth. */
    val heading: Flow<Float> = callbackFlow {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val r = FloatArray(9)
        val o = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(r, e.values)
                SensorManager.getOrientation(r, o)
                val deg = (Math.toDegrees(o[0].toDouble()).toFloat() + 360f) % 360f
                trySend(deg)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sm.unregisterListener(listener) }
    }

    /** 1Hz fused location; speed (m/s) doubles as a GPS fallback for the cluster. */
    @SuppressLint("MissingPermission") // requested in MainActivity; SecurityException handled below
    val location: Flow<android.location.Location> = callbackFlow {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                res.lastLocation?.let { trySend(it) }
            }
        }
        try {
            fused.requestLocationUpdates(req, cb, Looper.getMainLooper())
        } catch (_: SecurityException) { /* permission not granted yet — flow stays silent */ }
        awaitClose { fused.removeLocationUpdates(cb) }
    }
}
