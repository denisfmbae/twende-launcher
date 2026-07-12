package co.nedlink.twende.data.carlink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.nedlink.twende.model.CarLinkState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * CarLink 2.0 is a proprietary phone-mirroring stack; each head-unit vendor
 * ships its own SDK/broadcasts. This bridge is the documented mount point:
 * the manifest-registered receiver listens on a namespaced action so vendor
 * firmware (or an adb test) can drive the dashboard tile today, and the OEM
 * SDK calls slot into [attachVendorSdk] when the unit's SDK is available.
 *
 * Bench test:
 *   adb shell am broadcast -a co.nedlink.twende.carlink.SESSION_STATE \
 *       --ez connected true --es peer "Ned's Phone"
 */
class CarLinkBridge : BroadcastReceiver() {

    companion object {
        private val _state = MutableStateFlow(CarLinkState())
        val state: StateFlow<CarLinkState> = _state

        @Suppress("unused")
        fun attachVendorSdk(/* vendor session callbacks */) {
            // Wire the OEM SDK session listener to _state here.
        }
    }

    override fun onReceive(context: Context?, intent: Intent) {
        _state.value = CarLinkState(
            connected = intent.getBooleanExtra("connected", false),
            peer = intent.getStringExtra("peer"),
        )
    }
}
