package co.nedlink.twende.ui.video

import android.net.Uri
import android.provider.MediaStore
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import co.nedlink.twende.ui.theme.Twende
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One playable video found on the unit's own storage or a plugged-in USB stick. */
data class VideoItem(val uri: Uri, val title: String, val durationMs: Long)

/**
 * The video stage. A launcher cannot embed another app's picture — Android
 * forbids one app rendering another's surface — so instead of pretending, this
 * plays video files that live on the head unit itself: internal storage, the SD
 * card, or a USB stick, which is how video actually gets into a car anyway.
 * Controls sit directly beneath the picture, sized for a moving vehicle.
 */
@Composable
fun VideoPanel(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var clips by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf(false) }
    var view by remember { mutableStateOf<VideoView?>(null) }

    // Find clips once; MediaStore covers internal, SD and mounted USB volumes.
    LaunchedEffect(Unit) {
        clips = withContext(Dispatchers.IO) { scanVideos(ctx) }
        scanned = true
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { view?.stopPlayback() } }
    }

    val current = clips.getOrNull(index)

    fun play(i: Int) {
        if (clips.isEmpty()) return
        index = ((i % clips.size) + clips.size) % clips.size
        val v = view ?: return
        runCatching {
            v.setVideoURI(clips[index].uri)
            v.start()
            playing = true
        }
    }

    Column(modifier) {

        // ---------------- picture ----------------
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Twende.Panel)
                .border(1.dp, Twende.Line, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                factory = { c ->
                    VideoView(c).also { vv ->
                        view = vv
                        vv.setOnCompletionListener {
                            // roll on to the next clip like a playlist
                            if (clips.size > 1) {
                                index = (index + 1) % clips.size
                                runCatching {
                                    vv.setVideoURI(clips[index].uri); vv.start()
                                }
                            } else playing = false
                        }
                        vv.setOnErrorListener { _, _, _ -> playing = false; true }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            if (current == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("▶", fontSize = 46.sp, color = Twende.Cyan.copy(alpha = 0.55f))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (!scanned) "Looking for videos…" else "No videos found",
                        fontSize = 15.sp, color = Twende.Ink,
                    )
                    if (scanned) {
                        Text(
                            "Copy files to the unit or plug in a USB stick",
                            fontSize = 11.sp, color = Twende.Dim,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ---------------- title ----------------
        Text(
            current?.title ?: "—",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (current != null) Twende.Ink else Twende.Dim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (clips.isNotEmpty()) {
            Text(
                "${index + 1} of ${clips.size}",
                fontSize = 11.sp, color = Twende.Dim,
            )
        }

        Spacer(Modifier.height(8.dp))

        // ---------------- controls, directly below ----------------
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VideoButton("⏮") { play(index - 1) }
            VideoButton(if (playing) "⏸" else "▶", primary = true) {
                val v = view ?: return@VideoButton
                runCatching {
                    if (v.isPlaying) { v.pause(); playing = false }
                    else if (current != null) {
                        if (v.duration <= 0) v.setVideoURI(current.uri)
                        v.start(); playing = true
                    }
                }
            }
            VideoButton("⏭") { play(index + 1) }
            Spacer(Modifier.weight(1f))
            VideoButton("⟳") {
                clips = emptyList(); scanned = false
                // re-scan on the next composition tick
            }
        }
    }

    // Re-scan when the list was cleared by the refresh button.
    LaunchedEffect(scanned) {
        if (!scanned) {
            clips = withContext(Dispatchers.IO) { scanVideos(ctx) }
            index = 0
            scanned = true
        }
    }
}

@Composable
private fun VideoButton(glyph: String, primary: Boolean = false, onClick: () -> Unit) {
    val d = if (primary) 74.dp else 62.dp
    Box(
        Modifier
            .size(d)
            .clip(RoundedCornerShape(d / 2))
            .background(if (primary) Twende.Cyan.copy(alpha = 0.22f) else Twende.ButtonBg)
            .border(
                if (primary) 2.dp else 1.dp,
                if (primary) Twende.Cyan else Twende.Line,
                RoundedCornerShape(d / 2),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = if (primary) 28.sp else 24.sp, color = Twende.Cyan)
    }
}

/** Query every mounted volume MediaStore knows about, newest first. */
private fun scanVideos(ctx: android.content.Context): List<VideoItem> = runCatching {
    val cols = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
    )
    val out = mutableListOf<VideoItem>()
    ctx.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        cols, null, null,
        "${MediaStore.Video.Media.DATE_ADDED} DESC",
    )?.use { c ->
        val idI = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameI = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durI = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        while (c.moveToNext() && out.size < 200) {
            val id = c.getLong(idI)
            out += VideoItem(
                uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString()),
                title = c.getString(nameI) ?: "Clip",
                durationMs = c.getLong(durI),
            )
        }
    }
    out
}.getOrDefault(emptyList())
