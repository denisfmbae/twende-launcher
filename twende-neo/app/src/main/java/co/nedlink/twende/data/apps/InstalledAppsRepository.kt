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
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    pkg = it.activityInfo.packageName,
                    icon = runCatching<Bitmap> { it.loadIcon(pm).toBitmap(96, 96) }.getOrNull(),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun launch(pkg: String) {
        ctx.packageManager.getLaunchIntentForPackage(pkg)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(it)
        }
    }
}
