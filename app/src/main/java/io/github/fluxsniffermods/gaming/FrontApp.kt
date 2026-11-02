package io.github.fluxsniffermods.gaming

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.github.fluxsniffermods.core.shell.AdbShell

/**
 * The app in front, for gaming mode: once it has stayed there a moment (passing windows, a
 * notification shade, a widget updating on the home screen, never switch it), as Android itself
 * has it resumed (asked through the shell); and a game swiped away from recents ends gaming mode.
 */
internal object FrontApp {
    /** Packages that are never an app in front (the system's own windows). */
    private val IGNORED = setOf("android", "com.android.systemui")
    /** A short wait so passing windows don't switch it. */
    private const val SETTLE_MS = 200L
    private const val RECENTS_CHECK_MS = 1_000L
    private const val RECENTS_CHECKS = 30

    @Volatile private var frontApp: String? = null
    @Volatile private var frontTask: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    /** One check at a time: a slow one can't land after a newer one. */
    private val worker = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var pending: Runnable? = null
    private var recentsWatch: Runnable? = null

    /**
     * Quickstep, the recent apps screen: the system's own launcher draws it, even with another
     * home app chosen. Gaming mode carries on through it, until the game is swiped away.
     */
    private fun quickstep(context: Context): Set<String> = runCatching {
        val home = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
        context.packageManager.queryIntentActivities(home, 0)
            .filter { it.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 }
            .map { it.activityInfo.packageName }
            .filter { it != "com.android.settings" }
            .toSet()
    }.getOrDefault(emptySet())

    /** An app came to the front: gaming mode follows it once it has stayed there a moment. */
    fun onAppInFront(context: Context, packageName: String?, needsConfirming: Boolean = false) {
        if (packageName == null || packageName in IGNORED || (packageName == context.packageName && needsConfirming)) return
        val app = context.applicationContext
        if (packageName in quickstep(app) && !needsConfirming) {
            pending?.let { handler.removeCallbacks(it) }
            watchRecents(app)
            return
        }
        stopRecentsWatch()
        if (!GameProfiles.enabled(app)) return
        pending?.let { handler.removeCallbacks(it) }
        pending = Runnable {
            worker.execute {
                if (!AdbShell.available()) return@execute
                val resumed = resumedActivity()
                val front = resumed?.first ?: if (needsConfirming) return@execute else packageName
                if (front in IGNORED || front in quickstep(app)) return@execute
                // Another app's screen inside the app in front's own task (a purchase, a sign-in):
                // still that app
                val task = resumed?.second
                if (task != null && task == frontTask && front != frontApp) return@execute
                frontTask = task; frontApp = front
                GameMode.onAppInFront(app, front)
            }
        }.also { handler.postDelayed(it, SETTLE_MS) }
    }

    /** Recents in front: whether the game is still among the recent apps, once a second for half a minute. */
    private fun watchRecents(app: Context) {
        stopRecentsWatch()
        val owner = GameMode.playing() ?: return
        var checks = 0
        val check = object : Runnable {
            override fun run() {
                val again = this
                worker.execute {
                    if (!AdbShell.available()) return@execute
                    val front = resumedActivity()?.first
                    if (front != null && front !in quickstep(app)) return@execute
                    val open = AdbShell.run("dumpsys activity recents | grep -c -F -e '$owner/' -e ':$owner' || true", 2_000)
                        ?.trim()?.toIntOrNull() ?: return@execute
                    // Swiped away: gaming mode ends with it, its screen put back
                    if (open == 0) GameMode.gone(app, owner)
                    else if (++checks < RECENTS_CHECKS) handler.postDelayed(again, RECENTS_CHECK_MS)
                }
            }
        }
        recentsWatch = check
        handler.postDelayed(check, RECENTS_CHECK_MS)
    }

    private fun stopRecentsWatch() {
        recentsWatch?.let { handler.removeCallbacks(it) }
        recentsWatch = null
    }

    private val resumed = Regex("(?:topResumedActivity|mResumedActivity|ResumedActivity)[^\\n]*?\\s([\\w.]+)/[^\\s}]*(?:\\s+t(\\d+))?")

    /** The package of the activity Android has resumed, asked through the shell. */
    fun resumedPackage(): String? = resumedActivity()?.first

    private fun resumedActivity(): Pair<String, Int?>? =
        AdbShell.run("dumpsys activity activities | grep -m 2 -E 'topResumedActivity|mResumedActivity'", 2_000)
            ?.let { dump -> resumed.find(dump)?.let { it.groupValues[1] to it.groupValues[2].toIntOrNull() } }
}
