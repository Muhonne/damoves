package com.example.soundboard

data class ExportRequest(val name: String, val start: Int, val end: Int)

/** Editor decisions independent of VideoView and the storage implementation. */
class EditorCore {
    sealed interface Action {
        data class Prepare(val duration: Int, val start: Int, val end: Int) : Action
        data object SourceFailed : Action
        data class SetStart(val value: Int) : Action
        data class SetEnd(val value: Int) : Action
        data class SetName(val value: String) : Action
        data object TogglePreview : Action
        data object PreviewEnded : Action
        data object Save : Action
        data class SaveFinished(val success: Boolean) : Action
        data object ClearError : Action
    }

    var duration = 1; private set
    var start = 0; private set
    var end = 1; private set
    var name = ""; private set
    var previewing = false; private set
    var saving = false; private set
    var saved = false; private set
    var error: String? = null; private set
    var sourceFailed = false; private set
    var prepared = false; private set
    var pendingSave: ExportRequest? = null; private set

    fun send(action: Action) {
        when (action) {
            is Action.Prepare -> {
                duration = action.duration.coerceAtLeast(1)
                start = action.start.coerceIn(0, duration - 1)
                end = action.end.coerceIn(start + 1, duration)
                previewing = false; sourceFailed = false; prepared = true; error = null
            }
            Action.SourceFailed -> { previewing = false; sourceFailed = true; prepared = false; error = "Could not open this video." }
            is Action.SetStart -> { start = action.value.coerceIn(0, end - 1); previewing = false }
            is Action.SetEnd -> { end = action.value.coerceIn(start + 1, duration); previewing = false }
            is Action.SetName -> name = action.value
            Action.TogglePreview -> if (!saving && prepared) { previewing = !previewing }
            Action.PreviewEnded -> previewing = false
            Action.Save -> {
                if (!saving && prepared) {
                    if (name.trim().isEmpty()) error = "Enter a move name"
                    else {
                        pendingSave = ExportRequest(name.trim(), start, end)
                        error = null; previewing = false; saving = true; saved = false
                    }
                }
            }
            is Action.SaveFinished -> if (saving) {
                saving = false; saved = action.success; pendingSave = null
                if (!action.success) error = "Could not save this video. Try an MP4 video."
            }
            Action.ClearError -> error = null
        }
    }
}
