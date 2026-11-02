package io.github.fluxsniffermods.gaming

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.fluxsniffermods.core.ui.FluxTheme
import io.github.fluxsniffermods.core.ui.SearchEntry
import io.github.fluxsniffermods.core.FluxPrefs
import io.github.fluxsniffermods.gaming.settings.GameControlsScreen
import io.github.fluxsniffermods.gaming.settings.GameModeScreen
import io.github.fluxsniffermods.gaming.settings.GamePhoneScreen
import io.github.fluxsniffermods.gaming.settings.GameSourcesScreen
import io.github.fluxsniffermods.gaming.settings.GamesAppSettingsScreen

/** Gaming mode's settings pages: its main page, then a page per group. */
enum class GamePage { MAIN, CONTROLS, SOURCES, GAMES_APP, PHONE }

/** Gaming mode's settings, a page each (from the Games app's ⚙ and the Quick Settings tile). */
class GameSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val page = runCatching { GamePage.valueOf(intent.getStringExtra(EXTRA_PAGE) ?: "") }.getOrDefault(GamePage.MAIN)
        setContent {
            FluxTheme {
                val back = { finish() }
                when (page) {
                    GamePage.MAIN -> GameModeScreen(onBack = back)
                    GamePage.CONTROLS -> GameControlsScreen(onBack = back)
                    GamePage.SOURCES -> GameSourcesScreen(onBack = back)
                    GamePage.GAMES_APP -> GamesAppSettingsScreen(onBack = back)
                    GamePage.PHONE -> GamePhoneScreen(onBack = back)
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PAGE = "page"

        fun open(context: Context, page: GamePage = GamePage.MAIN) {
            runCatching {
                context.startActivity(
                    Intent(context, GameSettingsActivity::class.java).putExtra(EXTRA_PAGE, page.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

/** Gaming's own settings, found by the search on its page; and whether its games are in the keyboard's quick launcher. */
internal object GameSearch {
    private const val KEY_IN_LAUNCHER = "games_in_keyboard_launcher"

    fun entries(context: Context): List<SearchEntry> {
        fun entry(title: Int, summary: Int?, page: GamePage) = SearchEntry(title, summary) { GameSettingsActivity.open(context, page) }
        return listOf(
            entry(R.string.game_mode_layout_title, R.string.game_remap_row_description, GamePage.CONTROLS),
            entry(R.string.games_app_open, R.string.games_app_open_description, GamePage.GAMES_APP),
            entry(R.string.game_mode_import, R.string.game_mode_import_description, GamePage.SOURCES),
            entry(R.string.game_mode_xbox_add, R.string.game_mode_xbox_add_description, GamePage.SOURCES),
            entry(R.string.game_mode_folders, R.string.game_mode_folders_description, GamePage.SOURCES),
            entry(R.string.game_mode_folder_add, R.string.game_mode_folder_add_description, GamePage.SOURCES),
            entry(R.string.game_mode_quick_launcher, R.string.game_mode_quick_launcher_description, GamePage.GAMES_APP),
            entry(R.string.games_app_controller_keys, R.string.games_app_controller_keys_description, GamePage.GAMES_APP),
            entry(R.string.games_pin_all, R.string.games_pin_all_description, GamePage.GAMES_APP),
            entry(R.string.phone_game_mode, R.string.phone_game_mode_description, GamePage.PHONE),
        )
    }

    /** Flux Keyboard's quick launcher lists the games (it asks for them over the link). */
    fun inKeyboardLauncher(context: Context): Boolean = FluxPrefs.get(context).getBoolean(KEY_IN_LAUNCHER, true)

    fun setInKeyboardLauncher(context: Context, on: Boolean) = FluxPrefs.get(context).edit().putBoolean(KEY_IN_LAUNCHER, on).apply()
}
