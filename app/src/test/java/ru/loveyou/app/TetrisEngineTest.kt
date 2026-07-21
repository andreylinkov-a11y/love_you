package ru.loveyou.app

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TetrisEngineTest {
    @Test
    fun newStateStartsPlayable() {
        val state = TetrisEngine.newState(7)
        assertFalse(state.gameOver)
        assertTrue(TetrisEngine.fits(state, state.active))
        assertEquals(1, state.level)
    }

    @Test
    fun movementNeverLeavesBoard() {
        var state = TetrisEngine.newState(8).copy(active = FallingPiece(Tetromino.O, row = 0, column = 0))
        repeat(20) { state = TetrisEngine.move(state, -1) }
        assertEquals(0, state.active.column)
        repeat(20) { state = TetrisEngine.move(state, 1) }
        assertTrue(state.active.cells().all { it.column in 0 until state.columns })
    }

    @Test
    fun hardDropLocksPiece() {
        val before = TetrisEngine.newState(9).copy(active = FallingPiece(Tetromino.O, row = 0, column = 4))
        val after = TetrisEngine.hardDrop(before, Random(3))
        assertEquals(4, after.board.size)
        assertTrue(after.score > before.score)
    }

    @Test
    fun completedRowIsCleared() {
        val filled = (0 until 8).map { Cell(19, it) }.toSet()
        val state = TetrisState(
            board = filled,
            active = FallingPiece(Tetromino.O, row = 18, column = 8),
            next = Tetromino.T
        )
        val result = TetrisEngine.lock(state, Random(2))
        assertEquals(1, result.clearedLines)
        assertEquals(1, result.state.lines)
        assertEquals(2, result.state.board.size)
        assertTrue(result.state.score >= 100)
    }

    @Test
    fun speedIncreasesWithLevel() {
        val low = TetrisEngine.newState(1).copy(level = 1)
        val high = low.copy(level = 8)
        assertTrue(high.dropDelayMs < low.dropDelayMs)
    }
}