package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.NowPlaying
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass

/**
 * The background-music panel. Appears only when something is actually playing.
 *
 * Transport buttons are drawn on a Canvas — three triangles and a couple of bars.
 * Pulling in material-icons-extended for skip/pause would add megabytes to an APK
 * whose entire selling point is that it's 2.8 MB and starts instantly.
 *
 * Touch targets are 52dp: this gets pressed by a driver, at speed, without looking.
 */
@Composable
fun NowPlayingBar(
    np: NowPlaying,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!np.active) return

    Row(
        modifier.fillMaxWidth().glass(16).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Album art, or a neon placeholder when we have no metadata rights.
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Twende.Panel),
            contentAlignment = Alignment.Center,
        ) {
            val art = np.art
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Canvas(Modifier.size(20.dp)) { drawNote(Twende.Cyan) }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                np.title.ifBlank { "Audio playing" },
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFFE8ECF1), maxLines = 1,
            )
            if (np.hasMetadataAccess) {
                Text(
                    listOfNotNull(
                        np.artist.ifBlank { null },
                        np.appLabel.ifBlank { null },
                    ).joinToString(" · "),
                    fontSize = 10.sp, color = Twende.Dim, maxLines = 1,
                )
            } else {
                Text(
                    "Tap for track info",
                    fontSize = 10.sp, color = Twende.Cyan, maxLines = 1,
                    modifier = Modifier.clickable { onGrantAccess() },
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        TransportButton(onClick = onPrevious) { drawPrevious(Twende.Cyan) }
        Spacer(Modifier.width(6.dp))
        TransportButton(onClick = onPlayPause, accent = true) {
            if (np.playing) drawPause(Twende.Cosmic) else drawPlay(Twende.Cosmic)
        }
        Spacer(Modifier.width(6.dp))
        TransportButton(onClick = onNext) { drawNext(Twende.Cyan) }
    }
}

@Composable
private fun TransportButton(
    onClick: () -> Unit,
    accent: Boolean = false,
    glyph: DrawScope.() -> Unit,
) {
    Box(
        Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (accent) Twende.Cyan else Color(0x1AFFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) { glyph() }
    }
}

/* ---------- glyphs: cheaper than an icon dependency, and they glow ---------- */

private fun DrawScope.triangle(left: Float, width: Float, color: Color, pointRight: Boolean) {
    val h = size.height
    val path = Path().apply {
        if (pointRight) {
            moveTo(left, 0f); lineTo(left + width, h / 2f); lineTo(left, h)
        } else {
            moveTo(left + width, 0f); lineTo(left, h / 2f); lineTo(left + width, h)
        }
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawPlay(color: Color) =
    triangle(size.width * 0.18f, size.width * 0.68f, color, pointRight = true)

private fun DrawScope.drawPause(color: Color) {
    val w = size.width * 0.22f
    drawRect(color, Offset(size.width * 0.16f, 0f), Size(w, size.height))
    drawRect(color, Offset(size.width * 0.62f, 0f), Size(w, size.height))
}

private fun DrawScope.drawNext(color: Color) {
    triangle(0f, size.width * 0.55f, color, pointRight = true)
    drawRect(color, Offset(size.width * 0.78f, 0f), Size(size.width * 0.18f, size.height))
}

private fun DrawScope.drawPrevious(color: Color) {
    drawRect(color, Offset(size.width * 0.04f, 0f), Size(size.width * 0.18f, size.height))
    triangle(size.width * 0.45f, size.width * 0.55f, color, pointRight = false)
}

/** Placeholder when there's no album art (or no metadata access). */
private fun DrawScope.drawNote(color: Color) {
    val r = size.width * 0.22f
    drawCircle(color, r, Offset(r, size.height - r))
    drawRect(color, Offset(size.width * 0.38f, 0f), Size(size.width * 0.12f, size.height - r))
}
