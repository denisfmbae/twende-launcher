package co.nedlink.twende.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.nedlink.twende.data.apps.InstalledAppsRepository
import co.nedlink.twende.data.apps.SystemShortcuts
import co.nedlink.twende.data.media.NowPlayingRepository
import co.nedlink.twende.model.Accessory
import co.nedlink.twende.model.AppEntry
import co.nedlink.twende.model.NowPlaying
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repo: InstalledAppsRepository,
    private val shortcuts: SystemShortcuts,
    private val media: NowPlayingRepository,
    prefsRepo: co.nedlink.twende.data.prefs.PrefsRepository,
) : ViewModel() {

    private val commuterPriority = listOf(
        "com.google.android.apps.maps", "com.waze",
        "com.google.android.apps.youtube.music", "com.spotify.music",
        "com.android.dialer", "com.google.android.dialer",
        "com.whatsapp", "com.android.settings",
    )

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    /**
     * Dock contents. If the driver has hand-picked apps in Setup they win, in
     * their saved order; otherwise the known-driving-apps heuristic fills it.
     */
    val commuterApps: kotlinx.coroutines.flow.StateFlow<List<AppEntry>> =
        kotlinx.coroutines.flow.combine(_apps, prefsRepo.prefs) { apps, p ->
            val picked = p.commuterCsv.split(',').filter { it.isNotBlank() }
            if (picked.isEmpty()) {
                apps.sortedBy { e ->
                    commuterPriority.indexOf(e.pkg).let { if (it < 0) 100 else it }
                }.take(8)
            } else picked.mapNotNull { pk -> apps.firstOrNull { it.pkg == pk } }
        }.stateIn(
            viewModelScope,
            kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(2000),
            emptyList(),
        )

    /* ---- accessories rail: Bluetooth, files, player, radio, system panels ---- */
    private val _accessories = MutableStateFlow<List<Accessory>>(emptyList())
    val accessories: StateFlow<List<Accessory>> = _accessories

    fun openAccessory(id: String) = shortcuts.open(id)

    /* ---- background media ---- */
    val nowPlaying: StateFlow<NowPlaying> = media.state

    fun mediaNext() = media.next()
    fun mediaPrevious() = media.previous()
    fun mediaPlayPause() = media.playPause()
    fun grantMediaAccess() = media.requestMetadataAccess()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _apps.value = repo.load()
        _accessories.value = shortcuts.list()
    }

    fun launch(pkg: String) = repo.launch(pkg)
}
