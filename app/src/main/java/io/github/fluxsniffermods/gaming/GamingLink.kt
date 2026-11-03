package io.github.fluxsniffermods.gaming

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.inputmethod.EditorInfo
import io.github.fluxsniffermods.core.display.ExtraScreen
import io.github.fluxsniffermods.core.link.FluxLink
import io.github.fluxsniffermods.core.link.FluxLinkProvider
import io.github.fluxsniffermods.core.link.LinkMethods
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * What the other Flux apps ask of gaming: Flux Keyboard says when a text field is typed in (so
 * keys type there, not play) and lists the games in its quick launcher; Flux Streaming lists them
 * in Moonlight with their art, and tells gaming mode where the game is while it streams.
 */
class GamingLink : FluxLinkProvider() {
    override fun answer(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        return when (method) {
            LinkMethods.GAMES -> games(launcher = arg == "launcher")
            LinkMethods.GAME_STATE -> Bundle().apply {
                putString("playing", GameMode.playing())
                putBoolean("keys", GameMode.passesKeys())
                putBoolean("sendsKeys", GameMode.sendsKeys())
                putBoolean("enabled", GameProfiles.enabled(context))
                // Flux Gaming's own Xbox player in front: its fields' keys and text are its own
                putBoolean("player", XboxPlayActivity.inFront)
            }
            LinkMethods.GAME_LAYOUT -> Bundle().apply {
                val controls = FluxLayout.Control.entries
                putStringArray("controls", controls.map { it.name }.toTypedArray())
                putIntArray("codes", controls.map { runCatching { FluxLayout.keyOf(it) }.getOrDefault(0) }.toIntArray())
                putBoolean("sendsKeys", GameMode.sendsKeys())
            }
            LinkMethods.GAME_EDITOR -> {
                when (arg) {
                    "input_started" -> GameMode.inputStarted()
                    "keyboard_requested" -> GameMode.keyboardRequested()
                    "editor_finished" -> GameMode.editorFinished()
                    "editor_started" -> GameMode.editorStarted(EditorInfo().apply {
                        packageName = extras?.getString("package")
                        inputType = extras?.getInt("inputType") ?: 0
                        fieldId = extras?.getInt("fieldId") ?: 0
                        hintText = extras?.getString("hint")
                    })
                }
                FluxLink.ok()
            }
            LinkMethods.GAME_STREAMING -> {
                val w = extras?.getInt("width") ?: 0
                val h = extras?.getInt("height") ?: 0
                GameMode.streaming(context, if (w > 0 && h > 0) w to h else null)
                FluxLink.ok()
            }
            LinkMethods.GAME_EXTRA_SCREEN -> {
                val id = extras?.getInt("id") ?: 0
                GameMode.extraScreen(context, if (id > 0) ExtraScreen.Screen(id, extras!!.getInt("width"), extras.getInt("height"), extras.getBoolean("desktop")) else null)
                FluxLink.ok()
            }
            LinkMethods.GAME_FRONT -> { arg?.let { GameMode.onAppInFront(context, it) }; FluxLink.ok() }
            LinkMethods.GAME_RECOVER -> { GameMode.recover(context); FluxLink.ok() }
            LinkMethods.GAME_SETTLE -> {
                val done = CountDownLatch(1)
                GameMode.afterScreen { done.countDown() }
                FluxLink.ok(done.await(SETTLE_S, TimeUnit.SECONDS))
            }
            LinkMethods.GAME_PLACE_KEYS -> Bundle().apply { putParcelable("intent", GameMode.placeKeys(context)) }
            else -> null
        }
    }

    /** Every game, as the Games app lists them, each with what starts it (through its launcher). */
    private fun games(launcher: Boolean): Bundle? {
        val context = context ?: return null
        if (launcher && !GameSearch.inKeyboardLauncher(context)) return Bundle()
        val games = GamesActivity.games(context)
        return Bundle().apply {
            putStringArray("ids", games.map { it.first.id }.toTypedArray())
            putStringArray("names", games.map { it.first.name }.toTypedArray())
            putStringArray("packages", games.map { it.first.packages.joinToString(",") }.toTypedArray())
            putStringArray("launches", games.map { it.second.toUri(Intent.URI_INTENT_SCHEME) }.toTypedArray())
            // Each game's own start (its app's, a launcher's or an emulator's), as it's set up
            putStringArray("own", games.map { it.first.launch.orEmpty() }.toTypedArray())
            putLongArray("art", games.map { runCatching { GameArt.coverVersion(context, it.first) }.getOrDefault(0L) }.toLongArray())
            putIntArray("streamHeights", games.map { runCatching { GameProfiles.effective(context, it.first).streamHeight }.getOrNull() ?: 0 }.toIntArray())
            // Xbox Cloud Gaming's library, as one entry: Flux Gaming's own player, opened through its launcher
            putString("xbox_launch", GameLaunchActivity.intentFor(context, GameLibrary.Game("XboxPlayActivity", context.packageName,
                launch = Intent(context, XboxPlayActivity::class.java).setData(Uri.parse(XboxPlayActivity.HOME)).toUri(Intent.URI_INTENT_SCHEME)))
                .toUri(Intent.URI_INTENT_SCHEME))
            putLong("xbox_art", runCatching { GameArt.coverVersion(context, GameArt.XBOX_TILE) }.getOrDefault(0L))
            putString("games_app", Intent().setComponent(ComponentName(context, GamesActivity::class.java)).toUri(Intent.URI_INTENT_SCHEME))
        }
    }

    /** A game's art (art/<id>/<cover|portrait>), as a PNG kept for a while. */
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = context ?: throw FileNotFoundException()
        if (mode != "r") throw FileNotFoundException("read only")
        val parts = uri.pathSegments
        if (parts.size != 3 || parts[0] != "art") throw FileNotFoundException(uri.toString())
        val id = parts[1]
        val kind = parts[2]
        val profile = if (id == GameArt.XBOX_TILE.id) GameArt.XBOX_TILE
            else GamesActivity.games(context).firstOrNull { it.first.id == id }?.first ?: throw FileNotFoundException(id)
        val version = runCatching { GameArt.coverVersion(context, profile) }.getOrDefault(0L)
        val dir = File(context.cacheDir, "link-art").apply { mkdirs() }
        val file = File(dir, "${(id + kind).hashCode().toUInt()}-$version.png")
        if (!file.isFile) {
            val art = when {
                profile === GameArt.XBOX_TILE || id == GameArt.XBOX_TILE.id -> GameArt.xboxTile(context)
                kind == "portrait" -> GameArt.loadPortrait(context, profile)
                else -> GameArt.load(context, profile)
            } ?: throw FileNotFoundException("no art")
            // Old versions of the same art go
            dir.listFiles { f -> f.name.startsWith("${(id + kind).hashCode().toUInt()}-") }?.forEach { it.delete() }
            file.outputStream().use { art.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private companion object {
        const val SETTLE_S = 8L
    }
}
