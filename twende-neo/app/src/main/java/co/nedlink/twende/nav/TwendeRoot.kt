package co.nedlink.twende.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import co.nedlink.twende.ui.apps.AppDrawerScreen
import co.nedlink.twende.ui.home.HomeScreen
import co.nedlink.twende.ui.music.MusicScreen
import co.nedlink.twende.ui.settings.SettingsScreen
import co.nedlink.twende.ui.theme.Twende
import co.nedlink.twende.vm.SettingsViewModel

sealed class Dest(val route: String, val label: String) {
    data object Home : Dest("home", "Drive")
    data object Apps : Dest("apps", "Apps")
    data object Settings : Dest("settings", "Setup")
    data object Music : Dest("music", "Music")
}

/**
 * Root navigation. The old always-on Material bottom bar is gone — the home
 * screen now carries its own three giant nav pills (APPS · HOME · SETUP), and
 * every other page has the big bottom-centre HOME. Fewer chrome layers, more
 * screen, bigger targets.
 */
@Composable
fun TwendeRoot() {
    val nav = rememberNavController()

    // Persisted look prefs drive the live theme object.
    val sv: SettingsViewModel = hiltViewModel()
    val p by sv.prefs.collectAsStateWithLifecycle()
    LaunchedEffect(p.accentIdx, p.glowIntensity) {
        Twende.accentIdx = p.accentIdx
        Twende.glowLevel = p.glowIntensity
    }

    fun goHome() { nav.popBackStack(Dest.Home.route, false) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(nav, startDestination = Dest.Home.route) {
            composable(Dest.Home.route) {
                HomeScreen(
                    onOpenApps = { nav.navigate(Dest.Apps.route) { launchSingleTop = true } },
                    onOpenSettings = { nav.navigate(Dest.Settings.route) { launchSingleTop = true } },
                    onOpenMusic = { nav.navigate(Dest.Music.route) { launchSingleTop = true } },
                )
            }
            composable(Dest.Apps.route) { AppDrawerScreen(onHome = { goHome() }) }
            composable(Dest.Settings.route) { SettingsScreen(onHome = { goHome() }) }
            composable(Dest.Music.route) { MusicScreen(onHome = { goHome() }) }
        }
    }
}
