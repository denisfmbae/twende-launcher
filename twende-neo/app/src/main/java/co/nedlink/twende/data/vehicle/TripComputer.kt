package co.nedlink.twende.data.vehicle

import co.nedlink.twende.data.prefs.PrefsRepository
import co.nedlink.twende.model.Prefs
import co.nedlink.twende.model.Telemetry
import co.nedlink.twende.model.TripStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Turns the raw telemetry stream into the numbers a driver actually cares about:
 * how far, how much fuel, how many shillings, how badly am I driving, how far can
 * I still go.
 *
 * Everything here is *derived*, not read from the car — the car reports speed and
 * a fuel percentage; it does not report "you have spent KES 340". Fed only while
 * ObdService is producing frames, which is itself foreground-gated, so this adds
 * no background cost.
 *
 * Honest limits:
 *  - Litres burned comes from the fuel-gauge percentage dropping. Fuel senders are
 *    coarse (many cars report in ~2-5% steps and slosh on hills), so this is a good
 *    running estimate over a trip, not a fiscal record.
 *  - Range-to-empty is our own estimate, not the car's own DTE computer.
 *  - Harsh-event thresholds are heuristics on 2 Hz speed samples, not an
 *    accelerometer-grade IMU.
 */
@Singleton
class TripComputer @Inject constructor(prefsRepo: PrefsRepository) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats

    @Volatile private var prefs = Prefs()

    private var lastMs = 0L
    private var lastSpeed = 0
    private var lastFuelPct = -1
    private var penalty = 0f

    init {
        scope.launch { prefsRepo.prefs.collect { prefs = it } }
    }

    /** Called by ObdService for every telemetry frame (~2 Hz). */
    fun onFrame(t: Telemetry) {
        val now = System.currentTimeMillis()
        if (lastMs == 0L) {
            lastMs = now; lastSpeed = t.speedKmh; lastFuelPct = t.fuelPct
            return
        }
        val dt = ((now - lastMs) / 1000f).coerceIn(0f, 5f)   // seconds; clamp over gaps
        lastMs = now
        if (dt <= 0f) return

        val s = _stats.value
        val km = s.distanceKm + t.speedKmh * dt / 3600f
        val idleSec = s.idleSeconds + if (t.idling) dt else 0f

        // Fuel burned = gauge drop x tank size.
        var litres = s.litresUsed
        if (lastFuelPct in 0..100 && t.fuelPct in 0..100 && t.fuelPct < lastFuelPct) {
            litres += (lastFuelPct - t.fuelPct) / 100f * prefs.tankLitres
        }
        if (t.fuelPct in 0..100) lastFuelPct = t.fuelPct

        // Driving style, from real speed deltas.
        val accel = (t.speedKmh - lastSpeed) / dt        // km/h per second
        var harsh = s.harshEvents
        if (accel > 9f) { harsh++; penalty += 4f }       // hard launch
        if (accel < -11f) { harsh++; penalty += 5f }     // hard braking
        if (t.rpm > 4200 && t.speedKmh < 25) penalty += 0.4f * dt   // revving while crawling
        if (t.idling) penalty += 0.05f * dt                         // engine running, going nowhere
        penalty = (penalty - 0.02f * dt).coerceAtLeast(0f)          // drive well and it forgives
        lastSpeed = t.speedKmh

        val consumption = if (km > 0.8f && litres > 0.05f) litres / km * 100f else 0f
        val effective = if (consumption > 0.5f) consumption else 8.5f   // sane default until data
        val litresLeft = (t.fuelPct.coerceIn(0, 100) / 100f) * prefs.tankLitres
        val range = if (effective > 0f) (litresLeft / effective * 100f).roundToInt() else 0

        // A typical petrol engine burns roughly 0.7 L/h just sitting there idling.
        val idleLitres = idleSec / 3600f * 0.7f

        _stats.value = TripStats(
            distanceKm = km,
            litresUsed = litres,
            costKes = litres * prefs.fuelPriceKes,
            idleSeconds = idleSec,
            idleCostKes = idleLitres * prefs.fuelPriceKes,
            harshEvents = harsh,
            ecoScore = (100f - penalty).coerceIn(0f, 100f).roundToInt(),
            consumptionL100 = consumption,
            rangeKm = range,
        )
    }

    fun reset() {
        lastMs = 0L; lastSpeed = 0; lastFuelPct = -1; penalty = 0f
        _stats.value = TripStats()
    }
}
