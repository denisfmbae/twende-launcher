package co.nedlink.twende.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import android.view.KeyEvent
import co.nedlink.twende.model.NowPlaying
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whatever is playing in the background — the phone over Bluetooth A2DP, a local
 * player, YouTube Music, the lot.
 *
 * Two tiers, and the difference matters:
 *
 *  - CONTROL (skip / pause) always works, with no permission at all.
 *    AudioManager.dispatchMediaKeyEvent posts the same key event a steering-wheel
 *    button does; the OS routes it to whichever app currently owns media focus.
 *
 *  - METADATA (title, artist, art) needs MediaSessionManager.getActiveSessions(),
 *    which Android gates behind notification-listener access. The user grants that
 *    by hand, once. Without it we still know that *something* is playing, via
 *    AudioManager.isMusicActive, so the panel appears — just without the title.
 *
 * Polled at 1.5s, foreground-only (same gate as OBD), so it costs nothing parked.
 */
@Singleton
class NowPlayingRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state

    private val foreground = MutableStateFlow(true)

    private val audio: AudioManager
        get() = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val sessions: MediaSessionManager
        get() = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private val listenerComponent = ComponentName(ctx, TwendeNotificationListener::class.java)

    fun setAppForeground(fg: Boolean) { foreground.value = fg }

    init {
        scope.launch {
            while (isActive) {
                foreground.first { it }      // suspends, zero cost, until we're on screen
                refresh()
                delay(1500)
            }
        }
    }

    /** True once the user has granted notification access in system settings. */
    fun hasMetadataAccess(): Boolean {
        val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners")
            ?: return false
        return flat.split(":").any {
            ComponentName.unflattenFromString(it)?.packageName == ctx.packageName
        }
    }

    /** Opens the system screen where notification access is granted. */
    fun requestMetadataAccess() {
        runCatching {
            ctx.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private fun controller(): MediaController? {
        if (!hasMetadataAccess()) return null
        return runCatching {
            val active = sessions.getActiveSessions(listenerComponent)
            active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: active.firstOrNull()
        }.getOrNull()
    }

    private fun refresh() {
        val access = hasMetadataAccess()
        val c = controller()

        if (c != null) {
            val md = c.metadata
            val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
            _state.value = NowPlaying(
                active = true,
                playing = playing,
                title = md?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
                    .ifBlank { "Unknown track" },
                artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                appLabel = appLabel(c.packageName),
                art = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART),
                hasMetadataAccess = true,
            )
        } else {
            // No metadata rights (or no session) — but we can still tell if sound is coming out.
            val playing = runCatching { audio.isMusicActive }.getOrDefault(false)
            _state.value = NowPlaying(
                active = playing,
                playing = playing,
                title = if (playing) "Audio playing" else "",
                artist = if (playing && !access) "grant access for track info" else "",
                hasMetadataAccess = access,
            )
        }
    }

    private fun appLabel(pkg: String?): String = runCatching {
        val pm = ctx.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg ?: return "", 0)).toString()
    }.getOrDefault("")

    /* ---------- transport ---------- */

    fun next() = control(KeyEvent.KEYCODE_MEDIA_NEXT) { it.skipToNext() }
    fun previous() = control(KeyEvent.KEYCODE_MEDIA_PREVIOUS) { it.skipToPrevious() }

    fun playPause() = control(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) { t ->
        if (_state.value.playing) t.pause() else t.play()
    }

    /**
     * Prefer the real session (precise), fall back to a media key event (universal).
     * The fallback is what makes the buttons work on day one, before the user has
     * granted anything — and it's the same path a steering-wheel control takes.
     */
    private fun control(keyCode: Int, action: (MediaController.TransportControls) -> Unit) {
        val c = controller()
        if (c != null) {
            runCatching { action(c.transportControls) }
        } else {
            runCatching {
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
        }
        scope.launch { delay(350); refresh() }   // reflect the new track promptly
    }
}
