package co.nedlink.twende.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.nedlink.twende.data.prefs.PrefsRepository
import co.nedlink.twende.model.Prefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: PrefsRepository) : ViewModel() {
    val prefs: StateFlow<Prefs> =
        repo.prefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(2000), Prefs())

    fun setMetric(v: Boolean) = viewModelScope.launch { repo.setMetric(v) }
    fun setSimulated(v: Boolean) = viewModelScope.launch { repo.setObdSimulated(v) }
    fun setElmMac(v: String) = viewModelScope.launch { repo.setElmMac(v) }
    fun setPlacesKey(v: String) = viewModelScope.launch { repo.setPlacesKey(v) }
    fun setGlow(v: Float) = viewModelScope.launch { repo.setGlow(v) }
}
