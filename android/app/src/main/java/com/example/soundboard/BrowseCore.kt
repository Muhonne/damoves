package com.example.soundboard

interface SoundPreferences {
    fun load(): Boolean
    fun save(enabled: Boolean)
}

data class BrowseMove(val id: String, val name: String, val imported: Boolean = false)
data class BrowseChoice(val id: String, val loop: Boolean)

/** UI-independent browse behavior; the headless host exercises this same class. */
class BrowseCore(
    private val soundPreferences: SoundPreferences? = null,
    private val chooseIndex: (Int) -> Int = { kotlin.random.Random.nextInt(it) },
) {
    sealed interface Action {
        data class ReplaceMoves(val moves: List<BrowseMove>) : Action
        data class Search(val query: String) : Action
        data class SetSound(val enabled: Boolean) : Action
        data object RestoreSound : Action
        data class Open(val id: String) : Action
        data object RollDice : Action
        data object ClearChoice : Action
        data class RequestDelete(val id: String) : Action
        data object CancelDelete : Action
        data object ConfirmDelete : Action
        data class DeleteFinished(val success: Boolean) : Action
    }

    var moves: List<BrowseMove> = emptyList()
        private set
    var query: String = ""
        private set
    var soundEnabled: Boolean = soundPreferences?.load() ?: true
        private set
    var choice: BrowseChoice? = null
        private set
    var pendingDelete: String? = null
        private set
    var deleting: Boolean = false
        private set
    var deleteError: String? = null
        private set
    val visibleMoves: List<BrowseMove>
        get() = moves.filter { it.name.contains(query.trim(), ignoreCase = true) }

    fun send(action: Action) {
        when (action) {
            is Action.ReplaceMoves -> { moves = action.moves.toList(); choice = null }
            is Action.Search -> { query = action.query; choice = null }
            is Action.SetSound -> { soundEnabled = action.enabled; soundPreferences?.save(action.enabled) }
            Action.RestoreSound -> soundPreferences?.let { soundEnabled = it.load() }
            is Action.Open -> choice = if (visibleMoves.any { it.id == action.id }) BrowseChoice(action.id, false) else null
            Action.RollDice -> {
                val index = if (moves.isEmpty()) -1 else chooseIndex(moves.size)
                choice = moves.getOrNull(index)?.let { BrowseChoice(it.id, true) }
            }
            Action.ClearChoice -> choice = null
            is Action.RequestDelete -> if (!deleting) {
                pendingDelete = moves.firstOrNull { it.id == action.id && it.imported }?.id
                deleteError = null
            }
            Action.CancelDelete -> if (!deleting) { pendingDelete = null }
            Action.ConfirmDelete -> if (pendingDelete != null) { deleting = true }
            is Action.DeleteFinished -> if (deleting) {
                val id = pendingDelete
                if (action.success) {
                    moves = moves.filterNot { it.id == id }
                    if (choice?.id == id) choice = null
                    deleteError = null
                } else deleteError = "Could not delete this clip."
                pendingDelete = null; deleting = false
            }
        }
    }
}
