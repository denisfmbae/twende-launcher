package co.nedlink.twende.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.model.Poi
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass

/** The mockup's "Try gas stations, ATMs…" pill: tap → predictive POI cards. */
@Composable
fun PoiSearchBar(
    pois: List<Poi>,
    searching: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(26)
                .clickable {
                    expanded = !expanded
                    if (expanded) onOpen()
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, tint = Twende.Cyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(10.dp))
            Text(
                "Try gas stations, ATMs…",
                color = Twende.Dim, fontSize = 14.sp, modifier = Modifier.weight(1f),
            )
            if (searching) CircularProgressIndicator(Modifier.size(16.dp), color = Twende.Cyan, strokeWidth = 2.dp)
        }

        AnimatedVisibility(expanded && pois.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                pois.take(4).forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().glass(14).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = Twende.Cyan.copy(alpha = if (p.offline) 0.6f else 1f), fontSize = 14.sp)
                            Text(p.address, color = Twende.Dim, fontSize = 11.sp, maxLines = 1)
                        }
                        Text(p.category.uppercase(), color = Twende.Magenta, fontSize = 9.sp, letterSpacing = 1.5.sp)
                    }
                }
            }
        }
    }
}
