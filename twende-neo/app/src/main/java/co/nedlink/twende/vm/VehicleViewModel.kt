package co.nedlink.twende.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.nedlink.twende.data.bt.BtStatusRepository
import co.nedlink.twende.data.carlink.CarLinkBridge
import co.nedlink.twende.data.location.HeadingProvider
import co.nedlink.twende.data.obd.ObdRepository
import co.nedlink.twende.data.poi.PoiRepository
import co.nedlink.twende.data.prefs.PrefsRepository
import co.nedlink.twende.data.vehicle.CarBodyRepository
import co.nedlink.twende.data.vehicle.TripComputer
import co.nedlink.twende.model.BodyStatus
import co.nedlink.twende.model.DtcReport
import co.nedlink.twende.model.SensorScan
import co.nedlink.twende.model.TripStats
import co.nedlink.twende.model.BtState
import co.nedlink.twende.model.Door
import co.nedlink.twende.model.Poi
import co.nedlink.twende.model.Prefs
import co.nedlink.twende.model.Telemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Every stream uses WhileSubscribed: the moment HomeScreen leaves composition
 * (user launched Maps), upstream sensors/BT/OBD collectors stop within 2s.
 * Combined with the ProcessLifecycleOwner gate in TwendeApp, background cost
 * rounds to zero.
 */
@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val obd: ObdRepository,
    heading: HeadingProvider,
    bt: BtStatusRepository,
    prefsRepo: PrefsRepository,
    private val body: CarBodyRepository,
    tripComputer: TripComputer,
    private val poiRepo: PoiRepository,
) : ViewModel() {

    private fun <T> kotlinx.coroutines.flow.Flow<T>.hot(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(2000), initial)

    val telemetry: StateFlow<Telemetry> = obd.telemetry.hot(Telemetry())
    val headingDeg: StateFlow<Float> = heading.heading.hot(0f)
    val btState: StateFlow<BtState> = bt.state.hot(BtState())
    val carLink = CarLinkBridge.state
    val prefs: StateFlow<Prefs> = prefsRepo.prefs.hot(Prefs())
    val bodyStatus: StateFlow<BodyStatus> = body.status.hot(BodyStatus())
    val trip: StateFlow<TripStats> = tripComputer.stats

    fun toggleDoor(door: Door) = body.toggle(door)
    fun closeAllDoors() = body.closeAll()

    /* ---- check-engine (OBD-II Mode 03) ---- */
    val dtc = MutableStateFlow(DtcReport())
    val scanningDtc = MutableStateFlow(false)

    /* ---- demo driving (simulator only) ---- */
    val demoDriving = MutableStateFlow(false)
    fun toggleDemoDriving() {
        demoDriving.value = !demoDriving.value
        obd.setDemoDriving(demoDriving.value)
    }

    /* ---- sensor scan ---- */
    val sensorScan = MutableStateFlow(SensorScan())
    val scanningSensors = MutableStateFlow(false)

    fun scanSensors() {
        if (scanningSensors.value) return
        viewModelScope.launch {
            scanningSensors.value = true
            sensorScan.value = obd.scanSensors() ?: SensorScan(scanned = true)
            scanningSensors.value = false
        }
    }

    fun scanDtcs() {
        if (scanningDtc.value) return
        viewModelScope.launch {
            scanningDtc.value = true
            dtc.value = obd.scanDtcs() ?: DtcReport(scanned = true)
            scanningDtc.value = false
        }
    }
    private val location = heading.location.hot(null)

    val pois = MutableStateFlow<List<Poi>>(emptyList())
    val searching = MutableStateFlow(false)

    fun searchPois() {
        viewModelScope.launch {
            searching.value = true
            val loc = location.value
            pois.value = poiRepo.search(
                lat = loc?.latitude ?: 0.047,      // Meru fallback until first GPS fix
                lng = loc?.longitude ?: 37.649,
                fuelPct = telemetry.value.fuelPct,
                hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                apiKey = prefs.value.placesKey,
            )
            searching.value = false
        }
    }
}
