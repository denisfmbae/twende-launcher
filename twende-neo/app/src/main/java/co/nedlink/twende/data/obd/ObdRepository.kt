package co.nedlink.twende.data.obd

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import co.nedlink.twende.data.prefs.PrefsRepository
import co.nedlink.twende.model.DtcReport
import co.nedlink.twende.model.Telemetry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Binds ObdService lazily and re-exposes its telemetry as a cold Flow. */
@Singleton
class ObdRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val prefsRepo: PrefsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val binderState = MutableStateFlow<ObdService.LocalBinder?>(null)

    init {
        // Keep the service configured with the latest prefs, whenever bound.
        scope.launch {
            prefsRepo.prefs.collectLatest { p ->
                binderState.value?.configure(p.obdSimulated, p.elmMac)
            }
        }
    }

    fun setAppForeground(fg: Boolean) {
        binderState.value?.setPolling(fg)
    }

    /** One-shot fault-code scan. Null if the service isn't bound yet. */
    suspend fun scanDtcs(): DtcReport? = binderState.value?.scanDtcs()

    val telemetry: Flow<Telemetry> = callbackFlow {
        val conn = object : ServiceConnection {
            var job: kotlinx.coroutines.Job? = null
            override fun onServiceConnected(name: ComponentName?, service: IBinder) {
                val binder = service as ObdService.LocalBinder
                binderState.value = binder
                job = launch { binder.telemetry.collect { trySend(it) } }
            }
            override fun onServiceDisconnected(name: ComponentName?) {
                binderState.value = null
                job?.cancel()
            }
        }
        ctx.bindService(Intent(ctx, ObdService::class.java), conn, Context.BIND_AUTO_CREATE)
        awaitClose {
            ctx.unbindService(conn)
            binderState.value = null
        }
    }
}
