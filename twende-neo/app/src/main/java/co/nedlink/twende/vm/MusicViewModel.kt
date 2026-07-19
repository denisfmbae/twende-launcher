package co.nedlink.twende.vm

import androidx.lifecycle.ViewModel
import co.nedlink.twende.data.music.LocalMusicRepository
import co.nedlink.twende.data.music.LocalPlayback
import co.nedlink.twende.data.music.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val local: LocalMusicRepository,
) : ViewModel() {
    val songs = MutableStateFlow<List<Song>>(emptyList())
    val playback: StateFlow<LocalPlayback> = local.state

    fun permission() = local.permission()
    fun hasPermission() = local.hasPermission()
    fun refresh() { songs.value = local.loadSongs() }
    fun play(index: Int) = local.play(index)
    fun toggle() = local.toggle()
    fun stop() = local.stop()
}
