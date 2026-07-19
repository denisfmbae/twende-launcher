package co.nedlink.twende.ui.music

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import co.nedlink.twende.ui.common.BigHomeButton
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.vm.MusicViewModel

/**
 * Twende Music: every song MediaStore can see in the Music folder / SD card,
 * in one big-thumb list. Tap plays through the app's own player, and the home
 * transport bar controls it. Storage permission is requested in place.
 */
@Composable
fun MusicScreen(vm: MusicViewModel = hiltViewModel(), onHome: () -> Unit = {}) {
    val ctx = LocalContext.current
    val songs by vm.songs.collectAsState()
    val pb by vm.playback.collectAsState()

    LaunchedEffect(Unit) { if (vm.hasPermission()) vm.refresh() }

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TWENDE MUSIC", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Twende.Cyan)
                Spacer(Modifier.width(12.dp))
                Text(
                    if (songs.isEmpty()) "no songs found yet" else "${songs.size} songs",
                    fontSize = 12.sp, color = Twende.Dim,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "RESCAN",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Twende.Cyan,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Twende.ButtonBg)
                        .clickable { vm.refresh() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            if (!vm.hasPermission()) {
                Column(
                    Modifier.fillMaxWidth().glass(18).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Twende needs permission to read music files on this unit.",
                        fontSize = 15.sp, color = Twende.Ink,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "GRANT STORAGE ACCESS",
                        fontSize = 16.sp, fontWeight = FontWeight.Black, color = Twende.Cyan,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(2.dp, Twende.Cyan, RoundedCornerShape(20.dp))
                            .clickable {
                                (ctx as? Activity)?.let {
                                    ActivityCompat.requestPermissions(it, arrayOf(vm.permission()), 77)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Then tap RESCAN.", fontSize = 12.sp, color = Twende.Dim)
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp),
            ) {
                itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                    val current = pb.index == i && pb.active
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .glass(14)
                            .clickable { vm.play(i) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(14.dp).clip(CircleShape)
                                .background(if (current) Twende.Cyan else Twende.ButtonBg),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                song.title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                color = if (current) Twende.Cyan else Twende.Ink, maxLines = 1,
                            )
                            if (song.artist.isNotBlank()) {
                                Text(song.artist, fontSize = 12.sp, color = Twende.Dim, maxLines = 1)
                            }
                        }
                        val mins = song.durationMs / 60000
                        val secs = (song.durationMs / 1000) % 60
                        Text("%d:%02d".format(mins, secs), fontSize = 12.sp, color = Twende.Dim)
                    }
                }
            }
        }

        BigHomeButton(
            onClick = onHome,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        )
    }
}
