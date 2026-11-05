package com.example.soundboard

/** Playback decisions; the platform owns the actual player and release. */
class PlayerCore(val loop: Boolean) {
    sealed interface Action {
        data object Completed : Action
        data object Close : Action
        data object Leave : Action
        data object Failed : Action
    }

    var closed = false; private set
    var error: String? = null; private set

    fun send(action: Action) {
        if (closed) return
        when (action) {
            Action.Completed -> if (!loop) closed = true
            Action.Close, Action.Leave -> closed = true
            Action.Failed -> { closed = true; error = "Could not play this video." }
        }
    }
}
