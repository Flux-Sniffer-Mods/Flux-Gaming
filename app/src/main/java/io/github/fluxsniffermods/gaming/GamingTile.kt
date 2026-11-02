package io.github.fluxsniffermods.gaming

import android.app.Activity
import android.os.Bundle
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Gaming mode on or off from Quick Settings; what it still needs, under it. A long-press opens its settings. */
class GamingTileService : TileService() {
    override fun onStartListening() = update()

    override fun onClick() {
        GameProfiles.setEnabled(this, !GameProfiles.enabled(this))
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val on = GameProfiles.enabled(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // Nothing to add when ready: the system says On or Off itself
        tile.subtitle = if (on) GamingNeeds.firstMissing(this) else null
        tile.updateTile()
    }

    companion object {
        fun refresh(context: android.content.Context) {
            runCatching { requestListeningState(context, android.content.ComponentName(context, GamingTileService::class.java)) }
        }
    }
}

/** The tile long-pressed: gaming mode's settings. */
class GamingTileSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameSettingsActivity.open(this)
        finish()
    }
}
