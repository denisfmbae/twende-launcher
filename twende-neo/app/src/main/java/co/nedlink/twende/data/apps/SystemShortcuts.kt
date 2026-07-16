package co.nedlink.twende.data.apps

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import co.nedlink.twende.model.Accessory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The accessories rail: Bluetooth, file manager, media player, radio, and the
 * system panels a head unit actually needs.
 *
 * Every tile is a list of candidate intents tried in order — the first one that
 * PackageManager can resolve wins. Aftermarket head units are wildly inconsistent
 * (some have no Files app, some ship a vendor FM radio under an unguessable
 * package name), so a tile that can't resolve is shown DIMMED rather than
 * pretending to work and throwing ActivityNotFoundException in your face at 80km/h.
 */
@Singleton
class SystemShortcuts @Inject constructor(@ApplicationContext private val ctx: Context) {

    private data class Item(
        val id: String,
        val label: String,
        val glyph: String,
        val intents: List<Intent>,
    )

    private fun app(category: String): Intent =
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)

    private fun pkg(vararg candidates: String): List<Intent> =
        candidates.mapNotNull { ctx.packageManager.getLaunchIntentForPackage(it) }

    private val catalog: List<Item> by lazy {
        listOf(
            Item("bluetooth", "Bluetooth", "BT", listOf(
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            )),
            Item("files", "Files", "DIR", listOf(
                // The stock Documents UI, opened at internal storage root.
                Intent(Intent.ACTION_VIEW).setDataAndType(
                    Uri.parse("content://com.android.externalstorage.documents/root/primary"),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                ),
                // Any third-party file manager that handles a generic browse.
                Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
                    .addCategory(Intent.CATEGORY_OPENABLE),
            )),
            Item("player", "Player", "MUS", listOf(
                app(Intent.CATEGORY_APP_MUSIC),
            )),
            Item("radio", "Radio", "FM", pkg(
                // Best-effort probe of the FM apps common on MTK/Allwinner head units.
                // Wrong guesses are harmless: the tile simply dims.
                "com.android.fmradio", "com.mediatek.fmradio", "com.mtk.fmradio",
                "com.autochips.fmradio", "com.hzbhd.radio", "com.ts.radio",
            )),
            Item("phone", "Phone", "TEL", listOf(
                Intent(Intent.ACTION_DIAL),
            )),
            Item("camera", "Camera", "CAM", listOf(
                // Cameras are launched by action, not an app category (there is no
                // CATEGORY_APP_CAMERA). This resolves to the default camera app.
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
            )),
            Item("gallery", "Gallery", "PIC", listOf(
                app(Intent.CATEGORY_APP_GALLERY),
            )),
            Item("browser", "Browser", "WEB", listOf(
                app(Intent.CATEGORY_APP_BROWSER),
            )),
            Item("equalizer", "Equalizer", "EQ", listOf(
                Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                    .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, ctx.packageName)
                    .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC),
            )),
            Item("sound", "Sound", "VOL", listOf(
                Intent(Settings.ACTION_SOUND_SETTINGS),
            )),
            Item("display", "Display", "SCR", listOf(
                Intent(Settings.ACTION_DISPLAY_SETTINGS),
            )),
            Item("wifi", "Wi-Fi", "NET", listOf(
                Intent(Settings.ACTION_WIFI_SETTINGS),
            )),
            Item("storage", "Storage", "SD", listOf(
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
            )),
            Item("system", "All settings", "SYS", listOf(
                Intent(Settings.ACTION_SETTINGS),
            )),
        )
    }

    private fun resolve(item: Item): Intent? =
        item.intents.firstOrNull { it.resolveActivity(ctx.packageManager) != null }

    /** The rail, with each tile marked available or not. */
    fun list(): List<Accessory> = catalog.map {
        Accessory(id = it.id, label = it.label, glyph = it.glyph, available = resolve(it) != null)
    }

    fun open(id: String) {
        val item = catalog.firstOrNull { it.id == id } ?: return
        val intent = resolve(item) ?: return
        runCatching {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
