package com.example.soundboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/** Small JPEG previews cached on disk so tiles paint instantly on later launches. */
internal object ThumbnailStore {
    private const val DIRECTORY = "thumbs"

    private fun key(move: Move): String =
        move.uri?.let { "import_${importedMoveId(it)}" } ?: "res_${move.videoResId}"

    private fun file(context: Context, move: Move): File {
        val directory = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
        return File(directory, "${key(move)}.jpg")
    }

    fun load(context: Context, move: Move): Bitmap? {
        val file = file(context, move)
        if (!file.isFile) return null
        return BitmapFactory.decodeFile(file.path)
    }

    fun save(context: Context, move: Move, bitmap: Bitmap) {
        file(context, move).outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
    }

    fun clear(context: Context, move: Move) {
        file(context, move).delete()
    }
}
