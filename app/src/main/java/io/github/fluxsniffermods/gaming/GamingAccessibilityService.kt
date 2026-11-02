package io.github.fluxsniffermods.gaming

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import io.github.fluxsniffermods.core.FluxLog

/**
 * Gaming mode's keys and the app in front: every key passes here before the app (and the
 * keyboard) gets it, so in a game the keyboard plays it; and an app's screen coming to the front
 * picks its profile. Window changes only, never what's in them. Inert while gaming mode is off.
 */
class GamingAccessibilityService : AccessibilityService() {
    /** A failure with one key lets it through to the app as it is (and is logged), rather than ending the service. */
    override fun onKeyEvent(event: KeyEvent): Boolean = try {
        GameMode.onKeyEvent(event)
    } catch (e: Exception) {
        FluxLog.w(TAG, "key ${event.keyCode}: ${android.util.Log.getStackTraceString(e)}")
        false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        try {
            val pkg = event.packageName?.toString() ?: return
            val className = event.className?.toString()
            // Flux Streaming's page for the TV's screen: the game is on the TV, still in front there
            if (FrontApp.streamPage(pkg, className)) return
            // A window that isn't an app's own screen (a widget updating, recents) counts only
            // once Android confirms what's in front; one floating over the app keeps the app's
            val confirmed = isActivity(pkg, className)
            val floating = confirmed && isFloating(pkg, className)
            if (confirmed && !floating) GameMode.onWindow(pkg, className)
            if (!floating) FrontApp.onAppInFront(this, pkg, needsConfirming = !confirmed)
        } catch (e: Exception) {
            FluxLog.w(TAG, "window: ${android.util.Log.getStackTraceString(e)}")
        }
    }

    private val activities = HashMap<String, Boolean>()
    private val floatingActivities = HashMap<String, Boolean>()

    private fun isActivity(pkg: String, className: String?): Boolean {
        if (className.isNullOrEmpty()) return false
        return activities.getOrPut("$pkg/$className") {
            runCatching { packageManager.getActivityInfo(ComponentName(pkg, className), 0) }.isSuccess
        }
    }

    /** An activity drawn as a floating or see-through window, the app beneath still showing. */
    private fun isFloating(pkg: String, className: String?): Boolean {
        if (className.isNullOrEmpty()) return false
        return floatingActivities.getOrPut("$pkg/$className") {
            runCatching {
                val info = packageManager.getActivityInfo(ComponentName(pkg, className), 0)
                val theme = createPackageContext(pkg, 0).resources.newTheme().apply { applyStyle(info.themeResource, true) }
                val attrs = theme.obtainStyledAttributes(intArrayOf(android.R.attr.windowIsFloating, android.R.attr.windowIsTranslucent))
                try { attrs.getBoolean(0, false) || attrs.getBoolean(1, false) } finally { attrs.recycle() }
            }.getOrDefault(false)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        running = true
        // Gaming mode's double Back, in a game that ignores Back: the home screen
        GameMode.goHome = { performGlobalAction(GLOBAL_ACTION_HOME) }
        // Started with an app already in front (after an update, or just switched on)
        Thread {
            val front = FrontApp.resumedPackage() ?: return@Thread
            android.os.Handler(mainLooper).post { FrontApp.onAppInFront(this, front) }
        }.start()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        running = false
        GameMode.goHome = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FluxGamingA11y"
        @Volatile var running = false
            private set
    }
}
