package com.example.soundboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveSearchTest {
    private val crossBody = Move("Cross body lead", 1)
    private val turn = Move("Right turn", 2)
    private val doubleTurn = Move("Double right turn", 3)
    private val moves = listOf(crossBody, turn, doubleTurn)

    @Test
    fun searchMatchesPartialNamesIgnoringCaseAndSurroundingWhitespace() {
        assertEquals(listOf(turn, doubleTurn), filterMoves(moves, "  RIGHT  "))
    }

    @Test
    fun blankSearchRestoresAllMovesInTheirOriginalOrder() {
        assertEquals(moves, filterMoves(moves, "   "))
    }

    @Test
    fun unmatchedSearchReturnsNoMovesWithoutChangingTheLibrary() {
        assertTrue(filterMoves(moves, "missing").isEmpty())
        assertEquals(listOf(crossBody, turn, doubleTurn), filterMoves(moves, ""))
    }
}
