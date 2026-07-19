package co.nedlink.twende.data.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val durationMs: Long,
)

data class LocalPlayback(
    val active: Boolean = false,
    val playing: Boolean = false,
    val index: Int = -1,
    val title: String = "",
    val artist: String = "",
)

/**
 * Twende's own pocket jukebox: reads real files from the unit's Music folder /
 * SD card via MediaStore and plays them with a bare MediaPlayer. Exists because
 * many clone head units ship no controllable music app at all — this makes the
 * big transport buttons genuinely useful with nothing else installed.
 */
@Singleton
class LocalMusicRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val _state = MutableStateFlow(LocalPlayback())
    val state: StateFlow<LocalPlayback> = _state

    var queue: List<Song> = emptyList()
        private set

    private var player: MediaPlayer? = null

    fun permission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(ctx, permission()) == PackageManager.PERMISSION_GRANTED

    /** Scan the device for music files. Safe to call repeatedly. */
    fun loadSongs(): List<Song> {
        if (!hasPermission()) return emptyList()
        val out = mutableListOf<Song>()
        runCatching {
            val proj = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
            )
            ctx.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iT = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iA = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iD = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    out += Song(
                        id = id,
                        title = c.getString(iT) ?: "Unknown",
                        artist = (c.getString(iA) ?: "").takeIf { it != "<unknown>" } ?: "",
                        uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString()),
                        durationMs = c.getLong(iD),
                    )
                }
            }
        }
        queue = out
        return out
    }

    fun play(index: Int) {
        val song = queue.getOrNull(index) ?: return
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(ctx, song.uri)
                setOnCompletionListener { next() }
                setOnErrorListener { _, _, _ -> true }
                prepare()
                start()
            }
            _state.value = LocalPlayback(true, true, index, song.title, song.artist)
        }
    }

    fun toggle() {
        val p = player ?: return
        runCatching {
            if (p.isPlaying) { p.pause(); _state.value = _state.value.copy(playing = false) }
            else { p.start(); _state.value = _state.value.copy(playing = true) }
        }
    }

    fun next() { if (queue.isNotEmpty()) play((_state.value.index + 1).mod(queue.size)) }
    fun previous() { if (queue.isNotEmpty()) play((_state.value.index - 1).mod(queue.size)) }

    fun stop() {
        runCatching { player?.release() }
        player = null
        _state.value = LocalPlayback()
    }
}
