package com.example.soundboard

data class Move(val name: String, val videoResId: Int)

internal fun filterMoves(moves: List<Move>, query: String): List<Move> {
    val trimmedQuery = query.trim()
    return moves.filter { it.name.contains(trimmedQuery, ignoreCase = true) }
}

internal fun moveFromVideo(filename: String, resourceId: Int): Move? {
    val extension = filename.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
    if (extension !in setOf("mp4", "m4v", "webm", "3gp", "mkv")) return null
    val name = filename.substringBeforeLast('.').replace('_', ' ')
        .replaceFirstChar { it.uppercaseChar() }
    return Move(name, resourceId)
}
