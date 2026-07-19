package co.nedlink.twende.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.nedlink.twende.ui.common.BigHomeButton
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.vm.LauncherViewModel

// Fixed shelf order; only shelves with apps are shown.
private val SHELVES = listOf(
    "ALL", "MUSIC", "VIDEO", "NAV", "FINANCE", "SOCIAL", "GAMES", "PICS", "TOOLS",
)

/**
 * App drawer with a left category sidebar. Categories are derived per-app
 * (see InstalledAppsRepository.categoryOf) so the shelves populate themselves
 * on any unit. Big tiles fill the screen; the sidebar and tiles are all
 * thumb-sized for use in motion.
 */
@Composable
fun AppDrawerScreen(vm: LauncherViewModel = hiltViewModel(), onHome: () -> Unit = {}) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    var shelf by remember { mutableStateOf("ALL") }

    val counts = remember(apps) { apps.groupingBy { it.category }.eachCount() }
    val visibleShelves = remember(counts) {
        SHELVES.filter { it == "ALL" || (counts[it] ?: 0) > 0 }
    }
    val shown = remember(apps, shelf) {
        if (shelf == "ALL") apps else apps.filter { it.category == shelf }
    }

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Row(Modifier.fillMaxSize().padding(12.dp)) {

            // ---- category sidebar ----
            LazyColumn(
                Modifier.width(128.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 100.dp),
            ) {
                items(visibleShelves, key = { it }) { name ->
                    val selected = name == shelf
                    Column(
                        Modifier
                            .width(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Twende.Cyan.copy(alpha = 0.16f) else Twende.ButtonBg)
                            .clickable { shelf = name }
                            .padding(vertical = 14.dp, horizontal = 10.dp),
                    ) {
                        Text(
                            name, fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                            color = if (selected) Twende.Cyan else Twende.Ink,
                        )
                        Text(
                            if (name == "ALL") "${apps.size}" else "${counts[name] ?: 0}",
                            fontSize = 10.sp, color = Twende.Dim,
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // ---- app grid ----
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 110.dp),
            ) {
                itemsIndexed(shown, key = { i, a -> "${a.pkg}#$i" }) { _, app ->
                    Column(
                        Modifier.glass(16).clickable { vm.launch(app.pkg) }.padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        app.icon?.let {
                            Image(it.asImageBitmap(), app.label,
                                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
                        } ?: Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Twende.Panel))
                        Spacer(Modifier.height(8.dp))
                        Text(app.label, fontSize = 14.sp, color = Twende.Ink, maxLines = 1)
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
