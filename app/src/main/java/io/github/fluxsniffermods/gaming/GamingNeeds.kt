package io.github.fluxsniffermods.gaming

import android.content.Context
import io.github.fluxsniffermods.core.ui.AppSettingsActivity
import io.github.fluxsniffermods.core.ui.Need
import io.github.fluxsniffermods.core.ui.Needs

/** What gaming mode can't do without: the shell, its accessibility service, and the rest when used. */
internal object GamingNeeds {
    fun all(context: Context): List<Need> = listOf(
        Needs.shell(R.string.shell_access_for_gaming) { AppSettingsActivity.openShell(it) },
        Needs.accessibility(GamingAccessibilityService::class.java, R.string.readiness_accessibility_description),
        // Only for a game played by keys placed on its screen (the placer draws over it)
        Need(
            io.github.fluxsniffermods.core.R.string.readiness_overlay,
            { it.getString(R.string.readiness_overlay_description) },
            { ctx -> placesKeys(ctx) && !android.provider.Settings.canDrawOverlays(ctx) },
            { ctx -> Needs.overlay(R.string.readiness_overlay_description).fix(ctx) }
        ),
        Needs.notifications(R.string.readiness_notifications_gaming)
    )

    private fun placesKeys(context: Context): Boolean = runCatching {
        GameProfiles.all(context).any { it.taps.isNotEmpty() || it.streamTaps.isNotEmpty() }
    }.getOrDefault(false)

    /** The first thing still needed, as the Quick Settings tile's line (null when ready). */
    fun firstMissing(context: Context): String? =
        all(context).firstOrNull { runCatching { it.missing(context) }.getOrDefault(false) }?.let { context.getString(it.title) }
}
