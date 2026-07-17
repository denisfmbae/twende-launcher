package co.nedlink.twende.ui.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.nedlink.twende.ui.theme.CosmicBackground
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.ui.theme.glass
import co.nedlink.twende.vm.LauncherViewModel

@Composable
fun AppDrawerScreen(vm: LauncherViewModel = hiltViewModel()) {
    val apps by vm.apps.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        CosmicBackground()
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("ALL APPS · ${apps.size}", fontSize = 10.sp, letterSpacing = 3.sp, color = Twende.Dim)
            Spacer(Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                itemsIndexed(apps, key = { i, a -> "${a.pkg}#$i" }) { _, app ->
                    Column(
                        Modifier.glass(16).clickable { vm.launch(app.pkg) }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        app.icon?.let {
                            Image(it.asImageBitmap(), app.label,
                                Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)))
                        } ?: Box(Modifier.size(42.dp).clip(RoundedCornerShape(11.dp)).background(Twende.Panel))
                        Spacer(Modifier.height(6.dp))
                        Text(app.label, fontSize = 10.sp, color = Twende.Dim, maxLines = 1)
                    }
                }
            }
        }
    }
}
