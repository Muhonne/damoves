package com.example.soundboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImportedMoveTest {
    @Test
    fun readsIdFromStoredMoveUri() {
        assertEquals(
            "3f1c9a2e-4b7d-4f0a-9c1e-2b3d4e5f6a7b",
            importedMoveId("file:///data/user/0/com.example.soundboard/files/3f1c9a2e-4b7d-4f0a-9c1e-2b3d4e5f6a7b.mp4"))
    }

    @Test
    fun ignoresUrisWithoutAnMp4FileName() {
        assertNull(importedMoveId("file:///data/user/0/com.example.soundboard/files/abc123"))
        assertNull(importedMoveId("file:///data/user/0/com.example.soundboard/files/abc123.m4v"))
        assertNull(importedMoveId("file:///data/user/0/com.example.soundboard/files/"))
        assertNull(importedMoveId("file:///data/user/0/com.example.soundboard/files/.mp4"))
        assertNull(importedMoveId("abc.mp4"))
        assertNull(importedMoveId(""))
    }
}
