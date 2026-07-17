package co.nedlink.twende

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Catches any uncaught exception, writes the full stack trace to a file in the
 * app's own external files dir, and re-throws to the default handler.
 *
 * Retrieve without adb: the file lands at
 *   Android/data/co.nedlink.twende/files/last_crash.txt
 * reachable from the head unit's own File Manager. This exists purely so a crash
 * on hardware we can't attach a debugger to still tells us exactly what failed.
 */
object CrashCatcher {
    private const val FILE = "last_crash.txt"

    fun install(ctx: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                val text = buildString {
                    append("Twende crash\n")
                    append("thread: ").append(thread.name).append('\n')
                    append("time: ").append(System.currentTimeMillis()).append("\n\n")
                    append(sw.toString())
                }
                Log.e("TwendeCrash", text)
                ctx.getExternalFilesDir(null)?.let { dir ->
                    File(dir, FILE).writeText(text)
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** The last saved crash, or null. */
    fun lastCrash(ctx: Context): String? =
        ctx.getExternalFilesDir(null)?.let { dir ->
            File(dir, FILE).takeIf { it.exists() }?.readText()
        }
}
