package com.example.soundboard

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ThumbnailStoreTest {

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val bundled = Move("Preview", 999_001)
    private val importedId = "thumb-test-id"
    private val imported = Move(
        "Preview", 0, "file:///data/user/0/com.example.soundboard/files/$importedId.mp4")

    @After
    fun clearPreviews() {
        ThumbnailStore.clear(context, bundled)
        ThumbnailStore.clear(context, imported)
    }

    @Test
    fun savesAndReloadsASmallPreview() {
        val bitmap = Bitmap.createBitmap(36, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.RED)
        }
        ThumbnailStore.save(context, bundled, bitmap)
        val loaded = ThumbnailStore.load(context, bundled)
        assertNotNull(loaded)
        assertEquals(36, loaded!!.width)
        assertEquals(64, loaded.height)
    }

    @Test
    fun importedPreviewsAreKeyedByImportId() {
        ThumbnailStore.save(context, imported, Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888))
        val sameFile = Move(
            "Renamed preview", 0, "file:///data/user/0/com.example.soundboard/files/$importedId.mp4")
        assertNotNull(ThumbnailStore.load(context, sameFile))
    }

    @Test
    fun missingPreviewReturnsNull() {
        assertNull(ThumbnailStore.load(context, Move("Preview", 999_002)))
    }
}
