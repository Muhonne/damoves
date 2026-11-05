package com.example.soundboard.behavior

import com.example.soundboard.BrowseCore
import com.example.soundboard.BrowseMove
import com.example.soundboard.EditorCore
import com.example.soundboard.PlayerCore
import com.example.soundboard.NavigationCore
import com.example.soundboard.SoundPreferences
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

private class FixtureSound(var enabled: Boolean) : SoundPreferences {
    override fun load() = enabled
    override fun save(enabled: Boolean) { this.enabled = enabled }
}

private val json = GsonBuilder().serializeNulls().create()
private val actions = listOf("search", "setSound", "open", "rollDice", "clearChoice", "requestDelete", "cancelDelete", "confirmDelete")
private val editorActions = listOf("setStart", "setEnd", "setName", "togglePreview", "previewEnded", "save", "completeSave", "clearError")
private val playerActions = listOf("completed", "close", "leave", "failed")
private val navigationActions = listOf("openPlayer", "openEditor", "close")

private fun string(obj: JsonObject, key: String): String {
    val value = obj.get(key) ?: error("Missing $key")
    require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "$key must be a string" }
    return value.asString
}

private fun bool(obj: JsonObject, key: String): Boolean {
    val value = obj.get(key) ?: error("Missing $key")
    require(value.isJsonPrimitive && value.asJsonPrimitive.isBoolean) { "$key must be a boolean" }
    return value.asBoolean
}

private fun integer(obj: JsonObject, key: String): Int {
    val value = obj.get(key) ?: error("Missing $key")
    require(value.isJsonPrimitive && value.asJsonPrimitive.isNumber) { "$key must be an integer" }
    return value.asBigDecimal.intValueExact()
}

fun main() {
    var preferences = FixtureSound(true)
    var diceIndex = 0
    var core = BrowseCore(soundPreferences = preferences) { 0 }
    var editor = EditorCore()
    var player = PlayerCore(false)
    var navigation = NavigationCore()
    var focusedNavigation = false
    var focusedEditor = false
    var focusedPlayer = false
    var saveSucceeds = true
    var deferSave = false
    var lastExport: Map<String, Any>? = null
    var exportCount = 0
    var deleteSucceeds = true
    generateSequence(::readlnOrNull).forEach { line ->
        val response: Any = try {
            val request = JsonParser.parseString(line).asJsonObject
            when (string(request, "op")) {
                "init" -> {
                    val items = request.getAsJsonArray("moves") ?: error("Missing moves")
                    val moves = items.map { element ->
                        val move = element.asJsonObject
                        BrowseMove(string(move, "id"), string(move, "name"), if (move.has("imported")) bool(move, "imported") else false)
                    }
                    require(moves.map { it.id }.distinct().size == moves.size) { "Duplicate move id" }
                    val picker = request.get("randomIndex")
                    require(picker != null && picker.isJsonPrimitive && picker.asJsonPrimitive.isNumber) {
                        "randomIndex must be an integer"
                    }
                    val index = picker.asBigDecimal.intValueExact()
                    val sound = bool(request, "soundEnabled")
                    val outcome = if (request.has("deleteSucceeds")) bool(request, "deleteSucceeds") else true
                    deleteSucceeds = outcome
                    preferences = FixtureSound(sound)
                    diceIndex = index
                    core = BrowseCore(soundPreferences = preferences) { index }
                    core.send(BrowseCore.Action.ReplaceMoves(moves))
                    focusedEditor = false; focusedPlayer = false; focusedNavigation = false
                }
                "restart" -> {
                    require(!focusedEditor && !focusedPlayer && !focusedNavigation) { "Restart requires Browse" }
                    val moves = core.moves
                    val index = diceIndex
                    core = BrowseCore(soundPreferences = preferences) { index }
                    core.send(BrowseCore.Action.ReplaceMoves(moves))
                }
                "initNavigation" -> {
                    navigation = NavigationCore()
                    focusedEditor = false; focusedPlayer = false; focusedNavigation = true
                }
                "initPlayer" -> {
                    player = PlayerCore(bool(request, "loop"))
                    focusedEditor = false; focusedPlayer = true; focusedNavigation = false
                }
                "initEditor" -> {
                    val duration = integer(request, "duration")
                    val name = string(request, "name")
                    val outcome = bool(request, "saveSucceeds")
                    val available = if (request.has("sourceAvailable")) bool(request, "sourceAvailable") else true
                    val deferred = if (request.has("deferSave")) bool(request, "deferSave") else false
                    require(duration > 0) { "duration must be positive" }
                    editor = EditorCore()
                    if (available) editor.send(EditorCore.Action.Prepare(duration, 0, duration))
                    else editor.send(EditorCore.Action.SourceFailed)
                    editor.send(EditorCore.Action.SetName(name))
                    saveSucceeds = outcome
                    deferSave = deferred
                    lastExport = null; exportCount = 0
                    focusedEditor = true; focusedPlayer = false; focusedNavigation = false
                }
                "ls" -> Unit
                "do" -> if (focusedNavigation) when (string(request, "action")) {
                    "openPlayer" -> navigation.send(NavigationCore.Action.OpenPlayer)
                    "openEditor" -> navigation.send(NavigationCore.Action.OpenEditor)
                    "close" -> navigation.send(NavigationCore.Action.Close)
                    else -> error("Unknown action")
                } else if (focusedPlayer) when (string(request, "action")) {
                    "completed" -> player.send(PlayerCore.Action.Completed)
                    "close" -> player.send(PlayerCore.Action.Close)
                    "leave" -> player.send(PlayerCore.Action.Leave)
                    "failed" -> player.send(PlayerCore.Action.Failed)
                    else -> error("Unknown action")
                } else if (focusedEditor) when (string(request, "action")) {
                    "setStart" -> editor.send(EditorCore.Action.SetStart(integer(request, "value")))
                    "setEnd" -> editor.send(EditorCore.Action.SetEnd(integer(request, "value")))
                    "setName" -> editor.send(EditorCore.Action.SetName(string(request, "value")))
                    "togglePreview" -> editor.send(EditorCore.Action.TogglePreview)
                    "previewEnded" -> editor.send(EditorCore.Action.PreviewEnded)
                    "save" -> {
                        val wasSaving = editor.saving
                        editor.send(EditorCore.Action.Save)
                        if (!wasSaving) editor.pendingSave?.let { request ->
                            lastExport = mapOf("name" to request.name, "start" to request.start, "end" to request.end); exportCount++
                            if (!deferSave) editor.send(EditorCore.Action.SaveFinished(saveSucceeds))
                        }
                    }
                    "completeSave" -> editor.send(EditorCore.Action.SaveFinished(saveSucceeds))
                    "clearError" -> editor.send(EditorCore.Action.ClearError)
                    else -> error("Unknown action")
                } else when (string(request, "action")) {
                    "search" -> core.send(BrowseCore.Action.Search(string(request, "value")))
                    "setSound" -> core.send(BrowseCore.Action.SetSound(bool(request, "enabled")))
                    "open" -> core.send(BrowseCore.Action.Open(string(request, "id")))
                    "rollDice" -> core.send(BrowseCore.Action.RollDice)
                    "clearChoice" -> core.send(BrowseCore.Action.ClearChoice)
                    "requestDelete" -> core.send(BrowseCore.Action.RequestDelete(string(request, "id")))
                    "cancelDelete" -> core.send(BrowseCore.Action.CancelDelete)
                    "confirmDelete" -> {
                        core.send(BrowseCore.Action.ConfirmDelete)
                        if (core.deleting) core.send(BrowseCore.Action.DeleteFinished(deleteSucceeds))
                    }
                    else -> error("Unknown action")
                }
                else -> error("Unknown operation")
            }
            if (focusedNavigation) mapOf("ok" to true, "name" to "Navigation", "actions" to navigationActions,
                "state" to mapOf("screen" to navigation.screen.name.lowercase()))
            else if (focusedPlayer) mapOf("ok" to true, "name" to "Player", "actions" to playerActions,
                "state" to mapOf("loop" to player.loop, "closed" to player.closed, "error" to player.error))
            else if (focusedEditor) mapOf("ok" to true, "name" to "Editor", "actions" to editorActions, "lastExport" to lastExport, "exportCount" to exportCount,
                "state" to mapOf("duration" to editor.duration, "start" to editor.start, "end" to editor.end,
                    "name" to editor.name, "previewing" to editor.previewing, "saving" to editor.saving,
                    "saved" to editor.saved, "error" to editor.error, "sourceFailed" to editor.sourceFailed, "prepared" to editor.prepared))
            else mapOf("ok" to true, "name" to "Browse", "actions" to actions,
                "state" to mapOf("query" to core.query, "soundEnabled" to core.soundEnabled,
                    "visibleMoves" to core.visibleMoves.map { mapOf("id" to it.id, "name" to it.name) },
                    "choice" to core.choice, "pendingDelete" to core.pendingDelete,
                    "deleting" to core.deleting, "deleteError" to core.deleteError))
        } catch (e: Exception) {
            mapOf("ok" to false, "error" to (e.message ?: "Invalid request"))
        }
        println(json.toJson(response))
    }
}
