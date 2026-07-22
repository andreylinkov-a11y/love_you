package ru.loveyou.app

import org.junit.Assert.*
import org.junit.Test

class SlidingPuzzleEngineTest {
    @Test fun generatedBoardsAreValidAndUnsolved() {
        repeat(30) { seed ->
            val state = SlidingPuzzleEngine.newState(8, seed)
            assertEquals((0 until state.level.size * state.level.size).toSet(), state.board.toSet())
            assertFalse(state.finished)
            assertFalse(SlidingPuzzleEngine.isSolved(state.board))
        }
    }

    @Test fun sameRowMoveShiftsWholeChain() {
        val level = PuzzleLevel(1, 3, 0, 10)
        val state = PuzzleState(level, listOf(1,2,3,4,5,6,0,7,8))
        val result = SlidingPuzzleEngine.move(state, 8)
        assertTrue(result.accepted)
        assertEquals(2, result.shiftedTiles)
        assertEquals(listOf(1,2,3,4,5,6,7,8,0), result.state.board)
        assertTrue(result.state.finished)
    }

    @Test fun diagonalMoveIsRejected() {
        val level = PuzzleLevel(1, 3, 0, 10)
        val state = PuzzleState(level, listOf(1,2,3,4,5,6,7,0,8))
        assertFalse(SlidingPuzzleEngine.move(state, 3).accepted)
    }

    @Test fun difficultyMovesToFourByFour() {
        assertEquals(3, SlidingPuzzleEngine.level(5).size)
        assertEquals(4, SlidingPuzzleEngine.level(6).size)
        assertTrue(SlidingPuzzleEngine.level(12).shuffleMoves > SlidingPuzzleEngine.level(6).shuffleMoves)
    }
}
