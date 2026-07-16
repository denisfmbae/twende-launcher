package co.nedlink.twende.data.vehicle

import android.content.Context
import co.nedlink.twende.data.prefs.PrefsRepository
import co.nedlink.twende.model.BodyStatus
import co.nedlink.twende.model.Door
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vehicle body state: doors, hood, boot.
 *
 * IMPORTANT — why this isn't read like the other telemetry: RPM, speed, fuel and
 * coolant are standard OBD-II PIDs, so the ELM327 reads them directly. Door-ajar
 * status is NOT part of the OBD-II standard. It lives on the body-control CAN bus
 * and is manufacturer-specific, so a generic dongle simply cannot see it.
 *
 * Sources, highest priority first:
 *   1. VEHICLE   — real sensors via [tryVehicleSensors]. Available only where a
 *                  door API actually exists: Android Automotive OS
 *                  (CarPropertyManager DOOR_POS, needs a system-signed app) or a
 *                  vendor CAN decoder that surfaces doors. Returns null on generic
 *                  aftermarket head units — which is what this launcher targets.
 *   2. MANUAL    — the driver taps a panel on the dashboard widget.
 *   3. SIMULATOR — a scripted demo so the feature is visible on the bench.
 *
 * The simulator only runs while the launcher is foregrounded (wired from
 * TwendeApp), so it honours the app's zero-background-cost rule.
 */
@Singleton
class CarBodyRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val prefsRepo: PrefsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _status = MutableStateFlow(BodyStatus())
    val status: StateFlow<BodyStatus> = _status

    private val foreground = MutableStateFlow(true)
    @Volatile private var lastManualMs = 0L

    fun setAppForeground(fg: Boolean) { foreground.value = fg }

    /** On-screen / Settings override. Takes precedence over the simulator for 12s. */
    fun setDoor(door: Door, open: Boolean) {
        lastManualMs = System.currentTimeMillis()
        _status.value = _status.value.withDoor(door, open, BodyStatus.Source.MANUAL)
    }

    fun toggle(door: Door) {
        val s = _status.value
        val current = when (door) {
            Door.FRONT_LEFT -> s.frontLeft
            Door.FRONT_RIGHT -> s.frontRight
            Door.REAR_LEFT -> s.rearLeft
            Door.REAR_RIGHT -> s.rearRight
            Door.HOOD -> s.hood
            Door.TRUNK -> s.trunk
        }
        setDoor(door, !current)
    }

    fun closeAll() {
        lastManualMs = System.currentTimeMillis()
        _status.value = BodyStatus(source = BodyStatus.Source.MANUAL)
    }

    init {
        scope.launch {
            val vehicle = tryVehicleSensors(ctx)
            if (vehicle != null) {
                vehicle.collect { _status.value = it }   // real sensors present
            } else {
                demoLoop()                                // simulator / manual only
            }
        }
    }

    /** Scripted door activity so the widget demonstrates without a real bus. */
    private suspend fun demoLoop() {
        val sequence = listOf(
            Door.FRONT_LEFT, Door.TRUNK, Door.FRONT_RIGHT,
            Door.HOOD, Door.REAR_LEFT, Door.REAR_RIGHT,
        )
        var i = 0
        while (scope.isActive) {
            foreground.first { it }                       // suspend (zero cost) until foreground
            val simulated = prefsRepo.prefs.first().obdSimulated
            val idle = System.currentTimeMillis() - lastManualMs > 12_000
            if (simulated && idle) {
                val d = sequence[i % sequence.size]; i++
                _status.value = _status.value.withDoor(d, true, BodyStatus.Source.SIMULATOR)
                delay(2500)
                _status.value = _status.value.withDoor(d, false, BodyStatus.Source.SIMULATOR)
            }
            delay(5000)
        }
    }

    /**
     * Real-sensor mount point. To wire Android Automotive OS:
     *
     *   val car = Car.createCar(ctx)
     *   val mgr = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
     *   mgr.registerCallback(cb, VehiclePropertyIds.DOOR_POS,
     *                        CarPropertyManager.SENSOR_RATE_ONCHANGE)
     *
     * then map each door area's position (0 = shut, >0 = ajar) into a BodyStatus
     * with Source.VEHICLE and emit it. That path needs the automotive SDK plus a
     * system-signed build with android.car.permission.CONTROL_CAR_DOORS, so it is
     * intentionally returned as null here to keep the generic APK buildable — and
     * to avoid pretending the dongle can read doors when it can't.
     */
    private fun tryVehicleSensors(@Suppress("UNUSED_PARAMETER") ctx: Context): Flow<BodyStatus>? = null
}
