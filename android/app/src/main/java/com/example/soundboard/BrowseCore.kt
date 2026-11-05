package com.example.soundboard

data class BrowseMove(val id: String, val name: String)
data class BrowseChoice(val id: String, val loop: Boolean)

/** UI-independent browse behavior; the headless host exercises this same class. */
class BrowseCore(private val chooseIndex: (Int) -> Int = { kotlin.random.Random.nextInt(it) }) {
    sealed interface Action {
        data class ReplaceMoves(val moves: List<BrowseMove>) : Action
        data class Search(val query: String) : Action
        data class SetSound(val enabled: Boolean) : Action
        data class Open(val id: String) : Action
        data object RollDice : Action
        data object ClearChoice : Action
    }

    var moves: List<BrowseMove> = emptyList()
        private set
    var query: String = ""
        private set
    var soundEnabled: Boolean = true
        private set
    var choice: BrowseChoice? = null
        private set
    val visibleMoves: List<BrowseMove>
        get() = moves.filter { it.name.contains(query.trim(), ignoreCase = true) }

    fun send(action: Action) {
        when (action) {
            is Action.ReplaceMoves -> { moves = action.moves.toList(); choice = null }
            is Action.Search -> { query = action.query; choice = null }
            is Action.SetSound -> soundEnabled = action.enabled
            is Action.Open -> choice = if (visibleMoves.any { it.id == action.id }) BrowseChoice(action.id, false) else null
            Action.RollDice -> {
                val index = if (moves.isEmpty()) -1 else chooseIndex(moves.size)
                choice = moves.getOrNull(index)?.let { BrowseChoice(it.id, true) }
            }
            Action.ClearChoice -> choice = null
        }
    }
}
