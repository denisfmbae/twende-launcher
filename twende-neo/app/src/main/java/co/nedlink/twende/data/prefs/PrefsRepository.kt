package co.nedlink.twende.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import co.nedlink.twende.model.Prefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsRepository @Inject constructor(private val store: DataStore<Preferences>) {

    private object K {
        val METRIC = booleanPreferencesKey("metric_units")
        val OBD_SIM = booleanPreferencesKey("obd_simulated")
        val ELM_MAC = stringPreferencesKey("elm_mac")
        val PLACES_KEY = stringPreferencesKey("places_key")
        val GLOW = floatPreferencesKey("glow_intensity")
    }

    val prefs: Flow<Prefs> = store.data.map { p ->
        Prefs(
            metricUnits = p[K.METRIC] ?: true,
            obdSimulated = p[K.OBD_SIM] ?: true,
            elmMac = p[K.ELM_MAC] ?: "",
            placesKey = p[K.PLACES_KEY] ?: "",
            glowIntensity = p[K.GLOW] ?: 1f,
        )
    }

    suspend fun setMetric(v: Boolean) = store.edit { it[K.METRIC] = v }
    suspend fun setObdSimulated(v: Boolean) = store.edit { it[K.OBD_SIM] = v }
    suspend fun setElmMac(v: String) = store.edit { it[K.ELM_MAC] = v }
    suspend fun setPlacesKey(v: String) = store.edit { it[K.PLACES_KEY] = v }
    suspend fun setGlow(v: Float) = store.edit { it[K.GLOW] = v }
}
