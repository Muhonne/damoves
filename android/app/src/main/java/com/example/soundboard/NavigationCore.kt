package com.example.soundboard

/** Screen navigation decisions; the platform presents activities and pickers. */
class NavigationCore {
    enum class Screen { BROWSE, PLAYER, EDITOR }
    sealed interface Action {
        data object OpenPlayer : Action
        data object OpenEditor : Action
        data object Close : Action
    }
    var screen = Screen.BROWSE; private set
    fun send(action: Action) {
        when (action) {
            Action.OpenPlayer -> if (screen == Screen.BROWSE) screen = Screen.PLAYER
            Action.OpenEditor -> if (screen == Screen.BROWSE) screen = Screen.EDITOR
            Action.Close -> screen = Screen.BROWSE
        }
    }
}
