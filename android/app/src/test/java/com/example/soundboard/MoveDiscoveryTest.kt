package com.example.soundboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoveDiscoveryTest {
    @Test
    fun shortenedReleasePathsKeepOriginalMoveNames() {
        assertEquals(Move("Inside turn rock go hip catch 2", 42),
            moveFromResource("inside_turn_rock_go_hip_catch_2", "res/2E.mp4", 42))
        assertEquals(Move("Carousel", 7),
            moveFromResource("carousel", "res/_h.mp4", 7))
        assertNull(moveFromResource("damoves_keep", "res/a.xml", 8))
    }

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
