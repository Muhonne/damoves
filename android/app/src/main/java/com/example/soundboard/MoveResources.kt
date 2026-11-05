package com.example.soundboard

import android.content.res.Resources
import android.util.TypedValue
import java.util.Locale

internal fun discoverMoves(resources: Resources): List<Move> =
    R.raw::class.java.fields.mapNotNull { field ->
        val id = field.getInt(null)
        val value = TypedValue()
        resources.getValue(id, value, true)
        moveFromResource(resources.getResourceEntryName(id), value.string.toString(), id)
    }.sortedBy { it.name.lowercase(Locale.ROOT) }
