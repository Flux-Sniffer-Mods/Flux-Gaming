package io.github.fluxsniffermods.gaming

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import io.github.fluxsniffermods.core.FluxApp
import io.github.fluxsniffermods.core.link.FluxLink
import io.github.fluxsniffermods.core.link.LinkMethods

/** What gaming asks of Flux Streaming (when it's installed): the stream, and the TV it plays on. */
internal object StreamingLink {
    class State(val enabled: Boolean, val session: Boolean, val separate: Boolean, val desktop: Boolean, val game: String?)

    fun installed(context: Context) = FluxApp.STREAMING.installed(context)

    /** How streaming is now; null without Flux Streaming. Blocking. */
    fun state(context: Context): State? = FluxLink.call(context, FluxApp.STREAMING, LinkMethods.STREAM_STATE)?.let {
        State(it.getBoolean("enabled"), it.getBoolean("session"), it.getBoolean("separate"), it.getBoolean("desktop"), it.getString("game"))
    }

    fun setEnabled(context: Context, on: Boolean) {
        FluxLink.call(context, FluxApp.STREAMING, LinkMethods.STREAM_ENABLE, if (on) "1" else "0")
    }

    /** Flux Streaming's page for the TV's screen, or its own page. */
    fun open(context: Context, activity: String) {
        runCatching {
            context.startActivity(Intent().setComponent(ComponentName(FluxApp.STREAMING.packageName, activity)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    enum class Prompt { PHONE, TV, ASK }

    /** Whether opening game [id] now goes to the TV, is asked about (and the TV's name), or just plays on the phone. Blocking. */
    fun tvPrompt(context: Context, id: String): Pair<Prompt, String> {
        val answer = FluxLink.call(context, FluxApp.STREAMING, LinkMethods.TV_PROMPT, id) ?: return Prompt.PHONE to ""
        val mode = when (answer.getString("mode")) { "tv" -> Prompt.TV; "ask" -> Prompt.ASK; else -> Prompt.PHONE }
        return mode to answer.getString("tv").orEmpty()
    }

    fun tvPlay(context: Context, id: String, name: String) {
        FluxLink.call(context, FluxApp.STREAMING, LinkMethods.TV_PLAY, id, android.os.Bundle().apply { putString("name", name) })
    }

    fun volumeHere(context: Context): Boolean = FluxLink.ask(context, FluxApp.STREAMING, LinkMethods.TV_VOLUME_HERE)

    fun volume(context: Context, up: Boolean) {
        FluxLink.call(context, FluxApp.STREAMING, LinkMethods.TV_VOLUME, if (up) "up" else "down")
    }
}
