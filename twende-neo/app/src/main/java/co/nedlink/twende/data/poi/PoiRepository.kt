package co.nedlink.twende.data.poi

import co.nedlink.twende.model.Poi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Predictive POI engine. The *query builder* is pure and unit-testable:
 * category priority is derived from fuel level and time of day. Network is
 * Places API (New) searchNearby via bare HttpURLConnection — no HTTP client
 * dependency, keeping the APK small. Blank key → graceful offline suggestions.
 */
@Singleton
class PoiRepository @Inject constructor() {

    data class Category(val type: String, val label: String)

    /** Fuel below a quarter tank? Fuel wins. Meal hours? Food next. ATM rides along. */
    fun predictiveCategories(fuelPct: Int, hour: Int): List<Category> {
        val gas = Category("gas_station", "Gas stations")
        val food = Category("restaurant", "Restaurants")
        val atm = Category("atm", "ATMs")
        val mealTime = hour in 11..14 || hour in 18..21
        return when {
            fuelPct in 1..24 -> listOf(gas, atm, food)
            mealTime -> listOf(food, gas, atm)
            else -> listOf(gas, atm, food)
        }
    }

    suspend fun search(
        lat: Double, lng: Double,
        fuelPct: Int, hour: Int,
        apiKey: String,
    ): List<Poi> {
        val cats = predictiveCategories(fuelPct, hour)
        if (apiKey.isBlank()) return offlineSuggestions(cats)
        return withContext(Dispatchers.IO) {
            runCatching { nearby(lat, lng, cats.first(), apiKey) }
                .getOrElse { offlineSuggestions(cats) }
        }
    }

    private fun nearby(lat: Double, lng: Double, cat: Category, key: String): List<Poi> {
        val url = URL("https://places.googleapis.com/v1/places:searchNearby")
        val body = JSONObject()
            .put("includedTypes", listOf(cat.type))
            .put("maxResultCount", 6)
            .put("locationRestriction", JSONObject().put("circle", JSONObject()
                .put("center", JSONObject().put("latitude", lat).put("longitude", lng))
                .put("radius", 5000.0)))
            .toString()

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 4000
            readTimeout = 4000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("X-Goog-Api-Key", key)
            setRequestProperty("X-Goog-FieldMask", "places.displayName,places.formattedAddress")
        }
        conn.outputStream.use { it.write(body.toByteArray()) }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val places = JSONObject(text).optJSONArray("places") ?: return emptyList()
        return (0 until places.length()).map { i ->
            val p = places.getJSONObject(i)
            Poi(
                name = p.optJSONObject("displayName")?.optString("text") ?: "Unknown",
                address = p.optString("formattedAddress", ""),
                category = cat.label,
            )
        }
    }

    private fun offlineSuggestions(cats: List<Category>): List<Poi> =
        cats.map { Poi(name = it.label, address = "Add a Places API key in Setup for live results", category = it.label, offline = true) }
}
