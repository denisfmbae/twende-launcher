package co.nedlink.twende

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import co.nedlink.twende.data.media.NowPlayingRepository
import co.nedlink.twende.data.obd.ObdRepository
import co.nedlink.twende.data.vehicle.CarBodyRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * App-scope lifecycle gate: when Twende drops to the background (a launched app
 * takes over the screen), every non-critical poller is paused. OBD polling,
 * sensors and location all stop; they resume the instant we regain foreground.
 */
@HiltAndroidApp
class TwendeApp : Application() {

    @Inject lateinit var obdRepository: ObdRepository
    @Inject lateinit var carBodyRepository: CarBodyRepository
    @Inject lateinit var nowPlayingRepository: NowPlayingRepository

    override fun onCreate() {
        super.onCreate()
        CrashCatcher.install(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                obdRepository.setAppForeground(true)
                carBodyRepository.setAppForeground(true)
                nowPlayingRepository.setAppForeground(true)
            }
            override fun onStop(owner: LifecycleOwner) {
                obdRepository.setAppForeground(false)
                carBodyRepository.setAppForeground(false)
                nowPlayingRepository.setAppForeground(false)
            }
        })
    }
}
