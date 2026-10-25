package com.example.soundboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoveDiscoveryTest {
    @Test
    fun videoFilenameBecomesASentenceCaseMoveName() {
        assertEquals(Move("Cross body lead", 42), moveFromVideo("cross_body_lead.mp4", 42))
        assertEquals(Move("Turn", 7), moveFromVideo("turn.mp4", 7))
    }

    @Test
    fun supportedVideoExtensionsAreRecognized() {
        for (extension in listOf("mp4", "m4v", "webm", "3gp", "mkv", "MP4")) {
            assertEquals(Move("Turn", 7), moveFromVideo("turn.$extension", 7))
        }
    }

    @Test
    fun nonVideoResourcesAreIgnored() {
        for (filename in listOf("notes.txt", "cover.png", "music.mp3", "no_extension")) {
            assertNull(moveFromVideo(filename, 7))
        }
    }
}
