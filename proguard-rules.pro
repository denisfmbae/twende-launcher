package co.nedlink.twende.data.bt

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import co.nedlink.twende.model.BtState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HFP (calls) + A2DP (audio streaming) connection state and, where the remote
 * device reports it, battery level. Actual pairing lives in the unit's system
 * settings — a launcher should observe, not own, the BT stack.
 *
 * Battery uses the de-facto broadcast `...action.BATTERY_LEVEL_CHANGED`; most
 * head-unit ROMs and phones emit it, but it is OEM-dependent — treat as bonus.
 */
@Singleton
class BtStatusRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    private companion object {
        const val ACTION_BATTERY = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY = "android.bluetooth.device.extra.BATTERY_LEVEL"
    }

    val state: Flow<BtState> = callbackFlow {
        val adapter: BluetoothAdapter? =
            (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

        var hfp: BluetoothProfile? = null
        var a2dp: BluetoothProfile? = null
        var battery: Int? = null

        fun snapshot(): BtState = runCatching {
            val hfpDev = hfp?.connectedDevices?.firstOrNull()
            val a2dpDev = a2dp?.connectedDevices?.firstOrNull()
            val dev = hfpDev ?: a2dpDev
            BtState(
                deviceName = dev?.name,
                hfpConnected = hfpDev != null,
                a2dpConnected = a2dpDev != null,
                batteryPct = battery,
            )
        }.getOrDefault(BtState()) // SecurityException before permission grant → empty state

        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.HEADSET) hfp = proxy
                if (profile == BluetoothProfile.A2DP) a2dp = proxy
                trySend(snapshot())
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.HEADSET) hfp = null
                if (profile == BluetoothProfile.A2DP) a2dp = null
                trySend(snapshot())
            }
        }
        adapter?.getProfileProxy(ctx, listener, BluetoothProfile.HEADSET)
        adapter?.getProfileProxy(ctx, listener, BluetoothProfile.A2DP)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent) {
                if (i.action == ACTION_BATTERY) {
                    battery = i.getIntExtra(EXTRA_BATTERY, -1).takeIf { it in 0..100 }
                }
                trySend(snapshot())
            }
        }
        ctx.registerReceiver(receiver, IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(ACTION_BATTERY)
        })

        trySend(snapshot())
        awaitClose {
            ctx.unregisterReceiver(receiver)
            adapter?.closeProfileProxy(BluetoothProfile.HEADSET, hfp)
            adapter?.closeProfileProxy(BluetoothProfile.A2DP, a2dp)
        }
    }
}
