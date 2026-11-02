package io.github.fluxsniffermods.gaming

import android.app.Application
import io.github.fluxsniffermods.core.FluxCore
import io.github.fluxsniffermods.core.display.ExtraScreen

class GamingApp : Application() {
    private companion object {
        /** Gaming's settings as Flux Titan Hub kept them (the keyboard's quick launcher switch comes as its own). */
        val MIGRATED = listOf("flux_layout_", "game_", "gamehub_", "games_app_", "xbox_")
    }

    override fun onCreate() {
        super.onCreate()
        FluxCore.init(
            this,
            BuildConfig.VERSION_NAME,
            about = FluxCore.About(
                credits = R.string.gaming_credits,
                crashSections = { context ->
                    // Where games are found: the game folders, every Steam library and the launchers' own folders
                    "echo '== game folders =='; find '${GameFolders.ROOT}' -maxdepth 4 ! -name '.nomedia' 2>/dev/null | head -n 200; " +
                        "echo '== steam libraries =='; find /storage/emulated/0 -maxdepth 7 -name 'appmanifest_*.acf' 2>/dev/null | head -n 60; " +
                        "for p in com.xiaoji.egggame gamehub.lite; do pm path \$p >/dev/null 2>&1 && { echo \"== \$p ==\"; " +
                        "dumpsys package \$p | grep -E 'Activity|Action:|Scheme:|Authority:|Path' | grep -v -i 'permission' | head -n 150; }; done; " +
                        "echo '== input devices =='; dumpsys input | grep -E '^ +Device -?[0-9]+:' | head -30; " +
                        "echo \"rotation=\$(settings get system user_rotation | cat) auto=\$(settings get system accelerometer_rotation | cat)\"; "
                },
                crashTags = "FluxGameMode|FluxGameLaunch|FluxGamingA11y|gamehub|egggame"
            )
        )
        // Its own screens the shell can't open directly go through its game launcher, which it can
        ExtraScreen.launcher = GameLauncher
        // Gaming mode's keys, told to the keyboard as they're taken and given back
        GameMode.onKeysChanged = { FluxLinkTopics.keysChanged(this) }
        // Once: the games and profiles Flux Titan Hub kept, taken over from Flux Keyboard
        val app = this
        Thread {
            if (io.github.fluxsniffermods.core.link.FluxMigration.importOnce(app, MIGRATED)) {
                GameMode.profilesChanged(app)
                GamesActivity.sync(app)
            }
        }.start()
    }
}

/** How the extra screen opens gaming's own screens: through its game launcher. */
internal object GameLauncher : ExtraScreen.Launcher {
    override fun isShortcut(component: android.content.ComponentName) = component.className == GameLaunchActivity::class.java.name
    override fun targetOf(context: android.content.Context, intent: android.content.Intent) = GameLaunchActivity.packageOf(context, intent)
    override fun wrap(context: android.content.Context, intent: android.content.Intent): android.content.Intent? {
        val component = intent.component ?: return null
        return GameLaunchActivity.intentFor(context, GameLibrary.Game(component.className.substringAfterLast('.'), component.packageName,
            launch = intent.toUri(android.content.Intent.URI_INTENT_SCHEME)))
    }
}

/** Notes the other Flux apps watch. */
internal object FluxLinkTopics {
    @Volatile private var lastKeys: Boolean? = null

    fun keysChanged(context: android.content.Context) {
        val now = GameMode.passesKeys()
        if (now == lastKeys) return
        lastKeys = now
        io.github.fluxsniffermods.core.link.FluxLink.changed(context, io.github.fluxsniffermods.core.link.LinkMethods.TOPIC_GAME_KEYS)
    }
}
