package co.nedlink.twende.data.apps

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import co.nedlink.twende.model.AppEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledAppsRepository @Inject constructor(@ApplicationContext private val ctx: Context) {

    /** Loaded once per drawer visit on IO; icons pre-rasterised to 96px bitmaps. */
    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map {
                val label = it.loadLabel(pm).toString()
                val pkg = it.activityInfo.packageName
                AppEntry(
                    label = label,
                    pkg = pkg,
                    icon = runCatching<Bitmap> { it.loadIcon(pm).toBitmap(96, 96) }.getOrNull(),
                    category = categoryOf(pkg, label),
                )
            }
            .sortedBy { it.label.lowercase() }
            // A device can expose two launcher activities under one package
            // (e.g. clone units with a stock + vendor Settings, both
            // com.android.settings). Compose LazyRow keys must be unique, so
            // collapse duplicates by package — this is what crashed v2-build-17.
            .distinctBy { it.pkg }
            .toList()
    }

    /**
     * Sort an app into a drawer shelf. Android's own category field only exists
     * from API 26, and clone-unit vendors rarely fill it anyway — so keyword
     * heuristics on package + label do the real work on this hardware.
     */
    private fun categoryOf(pkg: String, label: String): String {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            runCatching {
                when (ctx.packageManager.getApplicationInfo(pkg, 0).category) {
                    android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> return "MUSIC"
                    android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> return "VIDEO"
                    android.content.pm.ApplicationInfo.CATEGORY_GAME -> return "GAMES"
                    android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> return "PICS"
                    android.content.pm.ApplicationInfo.CATEGORY_MAPS -> return "NAV"
                    android.content.pm.ApplicationInfo.CATEGORY_SOCIAL,
                    android.content.pm.ApplicationInfo.CATEGORY_NEWS -> return "SOCIAL"
                }
            }
        }
        val t = (pkg + " " + label).lowercase()
        fun has(vararg k: String) = k.any { t.contains(it) }
        return when {
            has("music", "audio", "spotify", "deezer", "boomplay", "fm", "radio", "player") -> "MUSIC"
            has("video", "tube", "movie", "vlc", "netflix", "showmax", "mx ") -> "VIDEO"
            has("game", "play.games", "candy", "racing", "puzzle") -> "GAMES"
            has("photo", "gallery", "camera", "image", "pics", "snapseed") -> "PICS"
            has("pesa", "bank", "pay", "wallet", "equity", "kcb", "absa", "coop", "money", "loan", "finance", "tala", "branch") -> "FINANCE"
            has("map", "navi", "waze", "gps", "uber", "bolt", "little") -> "NAV"
            has("whats", "telegram", "chat", "messag", "facebook", "insta", "twitter", "tiktok", "x.android") -> "SOCIAL"
            else -> "TOOLS"
        }
    }

    fun launch(pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
        }
    }
}
