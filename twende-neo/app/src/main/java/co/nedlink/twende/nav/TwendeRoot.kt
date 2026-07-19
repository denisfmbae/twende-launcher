package co.nedlink.twende.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import co.nedlink.twende.ui.apps.AppDrawerScreen
import co.nedlink.twende.ui.home.HomeScreen
import co.nedlink.twende.ui.settings.SettingsScreen

sealed class Dest(val route: String, val label: String) {
    data object Home : Dest("home", "Drive")
    data object Apps : Dest("apps", "Apps")
    data object Settings : Dest("settings", "Setup")
}

@Composable
fun TwendeRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Dest.Home.route

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E1016), tonalElevation = 0.dp) {
                listOf(
                    Triple(Dest.Home, Icons.Filled.Home, "Drive"),
                    Triple(Dest.Apps, Icons.Filled.Menu, "Apps"),
                    Triple(Dest.Settings, Icons.Filled.Settings, "Setup"),
                ).forEach { (dest, icon, label) ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            nav.navigate(dest.route) {
                                popUpTo(Dest.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00E5FF),
                            selectedTextColor = Color(0xFF00E5FF),
                            indicatorColor = Color(0x3300E5FF),
                            unselectedIconColor = Color(0xFF5C6672),
                            unselectedTextColor = Color(0xFF5C6672),
                        )
                    )
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = Dest.Home.route, modifier = Modifier.padding(pad)) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Apps.route) { AppDrawerScreen(onHome = { nav.popBackStack(Dest.Home.route, false) }) }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
