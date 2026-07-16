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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repo: InstalledAppsRepository,
    private val shortcuts: SystemShortcuts,
    private val media: NowPlayingRepository,
) : ViewModel() {

    private val commuterPriority = listOf(
        "com.google.android.apps.maps", "com.waze",
        "com.google.android.apps.youtube.music", "com.spotify.music",
        "com.android.dialer", "com.google.android.dialer",
        "com.whatsapp", "com.android.settings",
    )

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps

    /** Dock contents: known driving apps first, then whatever's installed. */
    val commuterApps: List<AppEntry>
        get() = _apps.value.sortedBy { e ->
            commuterPriority.indexOf(e.pkg).let { if (it < 0) 100 else it }
        }.take(8)

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
