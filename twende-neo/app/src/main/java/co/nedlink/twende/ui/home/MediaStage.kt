package co.nedlink.twende.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.NowPlaying
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass

/**
 * The media stage — the top half of the home screen. Large album art on the
 * left, the track name at a size readable from the driver's seat, and transport
 * controls big enough to hit without aiming. When notification access hasn't
 * been granted it becomes an unmissable prompt instead, because that single
 * permission is the difference between "Audio playing" and the real song name.
 */
@Composable
fun MediaStage(
    np: NowPlaying,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onGrantAccess: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.glass(18).padding(14.dp)) {

        if (!np.hasMetadataAccess) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Twende.Cyan.copy(alpha = 0.14f))
                    .border(1.5.dp, Twende.Cyan, RoundedCornerShape(12.dp))
                    .clickable { onGrantAccess() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("♬", fontSize = 22.sp, color = Twende.Cyan)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "TAP TO ENABLE SONG NAMES",
                        fontSize = 13.sp, fontWeight = FontWeight.Black, color = Twende.Cyan,
                    )
                    Text(
                        "Grants track title, artwork and full control",
                        fontSize = 10.sp, color = Twende.Dim,
                    )
                }
                Text("›", fontSize = 24.sp, color = Twende.Cyan)
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

            // ---- album art / placeholder, square, fills the stage height ----
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Twende.Panel)
                    .border(1.dp, Twende.Line, RoundedCornerShape(14.dp))
                    .clickable { onOpenLibrary() },
                contentAlignment = Alignment.Center,
            ) {
                val art = np.art
                if (art != null) {
                    Image(
                        bitmap = art.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
                    )
                } else {
                    Text("♪", fontSize = 54.sp, color = Twende.Cyan.copy(alpha = 0.55f))
                }
            }

            Spacer(Modifier.width(16.dp))

            // ---- title + controls ----
            Column(Modifier.weight(1f)) {
                Text(
                    np.title.ifBlank { if (np.active) "Audio playing" else "Nothing playing" },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = if (np.active) Twende.Ink else Twende.Dim,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 36.sp,
                )
                if (np.artist.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(np.artist, fontSize = 15.sp, color = Twende.Dim, maxLines = 1)
                }

                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    StageButton("⏮", onPrevious)
                    StageButton(if (np.playing) "⏸" else "▶", onPlayPause, primary = true)
                    StageButton("⏭", onNext)
                    Spacer(Modifier.weight(1f))
                    StageButton("☰", onOpenLibrary)
                }
            }
        }
    }
}

@Composable
private fun StageButton(glyph: String, onClick: () -> Unit, primary: Boolean = false) {
    Box(
        Modifier
            .size(if (primary) 82.dp else 70.dp)
            .clip(RoundedCornerShape(if (primary) 41.dp else 35.dp))
            .background(if (primary) Twende.Cyan.copy(alpha = 0.22f) else Twende.ButtonBg)
            .border(
                if (primary) 2.dp else 1.dp,
                if (primary) Twende.Cyan else Twende.Line,
                RoundedCornerShape(if (primary) 41.dp else 35.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = if (primary) 32.sp else 26.sp, color = Twende.Cyan)
    }
}
