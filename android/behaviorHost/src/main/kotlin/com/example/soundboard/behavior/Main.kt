package com.example.soundboard.behavior

import com.example.soundboard.BrowseCore
import com.example.soundboard.BrowseMove
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser

private val json = GsonBuilder().serializeNulls().create()
private val actions = listOf("search", "setSound", "open", "rollDice", "clearChoice")

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

fun main() {
    var core = BrowseCore { 0 }
    generateSequence(::readlnOrNull).forEach { line ->
        val response: Any = try {
            val request = JsonParser.parseString(line).asJsonObject
            when (string(request, "op")) {
                "init" -> {
                    val items = request.getAsJsonArray("moves") ?: error("Missing moves")
                    val moves = items.map { element ->
                        val move = element.asJsonObject
                        BrowseMove(string(move, "id"), string(move, "name"))
                    }
                    require(moves.map { it.id }.distinct().size == moves.size) { "Duplicate move id" }
                    val picker = request.get("randomIndex")
                    require(picker != null && picker.isJsonPrimitive && picker.asJsonPrimitive.isNumber) {
                        "randomIndex must be an integer"
                    }
                    val index = picker.asBigDecimal.intValueExact()
                    val sound = bool(request, "soundEnabled")
                    core = BrowseCore { index }
                    core.send(BrowseCore.Action.ReplaceMoves(moves))
                    core.send(BrowseCore.Action.SetSound(sound))
                }
                "ls" -> Unit
                "do" -> when (string(request, "action")) {
                    "search" -> core.send(BrowseCore.Action.Search(string(request, "value")))
                    "setSound" -> core.send(BrowseCore.Action.SetSound(bool(request, "enabled")))
                    "open" -> core.send(BrowseCore.Action.Open(string(request, "id")))
                    "rollDice" -> core.send(BrowseCore.Action.RollDice)
                    "clearChoice" -> core.send(BrowseCore.Action.ClearChoice)
                    else -> error("Unknown action")
                }
                else -> error("Unknown operation")
            }
            mapOf("ok" to true, "name" to "Browse", "actions" to actions,
                "state" to mapOf("query" to core.query, "soundEnabled" to core.soundEnabled, "visibleMoves" to core.visibleMoves,
                    "choice" to core.choice))
        } catch (e: Exception) {
            mapOf("ok" to false, "error" to (e.message ?: "Invalid request"))
        }
        println(json.toJson(response))
    }
}
