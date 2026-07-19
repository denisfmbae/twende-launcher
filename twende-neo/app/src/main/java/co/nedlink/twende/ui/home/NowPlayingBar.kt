package co.nedlink.twende.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
 * The music panel — sized for a driver's thumb. Big title, 64dp transport
 * buttons, and a prominent ENABLE pill when notification access hasn't been
 * granted yet, because that grant is what turns these buttons from "hopeful
 * key events" into a real MediaController on most head units.
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
    Row(
        modifier.fillMaxWidth().glass(16).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)).background(Twende.Panel),
            contentAlignment = Alignment.Center,
        ) {
            val art = np.art
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)),
                )
            } else {
                Canvas(Modifier.size(26.dp)) { drawNote(Twende.Cyan) }
            }
        }

        Spacer(Modifier.width(14.dp))

        // One line, huge — readable from the driver's seat, stretching to centre
        // screen. The artist/app row is gone by request: title is the signal.
        Text(
            np.title.ifBlank { if (np.active) "Audio playing" else "Nothing playing" },
            fontSize = 30.sp, fontWeight = FontWeight.Black,
            color = if (np.active) Twende.Ink else Twende.Dim,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.width(10.dp))

        if (!np.hasMetadataAccess) {
            // The one-time unlock: opens the notification-access screen where the
            // driver flips Twende on. After that, track titles, art and precise
            // control of the playing app all light up.
            Box(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x2200E5FF))
                    .border(1.5.dp, Twende.Cyan, RoundedCornerShape(24.dp))
                    .clickable { onGrantAccess() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("ENABLE\nCONTROLS", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = Twende.Cyan, lineHeight = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
        }

        TransportButton(onClick = onPrevious) { drawPrevious(Twende.Cyan) }
        Spacer(Modifier.width(8.dp))
        TransportButton(onClick = onPlayPause, accent = true) {
            if (np.playing) drawPause(Twende.Cosmic) else drawPlay(Twende.Cosmic)
        }
        Spacer(Modifier.width(8.dp))
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
            .size(84.dp)
            .clip(CircleShape)
            .background(if (accent) Twende.Cyan else Twende.ButtonBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(36.dp)) { glyph() }
    }
}

/* ---------- glyphs ---------- */

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

private fun DrawScope.drawNote(color: Color) {
    val r = size.width * 0.22f
    drawCircle(color, r, Offset(r, size.height - r))
    drawRect(color, Offset(size.width * 0.38f, 0f), Size(size.width * 0.12f, size.height - r))
}
