package co.nedlink.twende.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.nedlink.twende.data.apps.InstalledAppsRepository
import co.nedlink.twende.model.AppEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val repo: InstalledAppsRepository,
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

    init { refresh() }

    fun refresh() = viewModelScope.launch { _apps.value = repo.load() }

    fun launch(pkg: String) = repo.launch(pkg)
}
