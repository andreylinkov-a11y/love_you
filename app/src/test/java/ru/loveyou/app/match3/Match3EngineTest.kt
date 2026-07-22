package ru.loveyou.app.match3

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Match3EngineTest {
    @Test
    fun newGameHasNoImmediateMatchesAndHasMove() {
        repeat(40) { seed ->
            val engine = Match3Engine(Random(seed))
            val state = engine.newGame((seed % 15) + 1)
            assertTrue(engine.findMatches(state.board, state.level).isEmpty())
            assertTrue(engine.hasPossibleMove(state.board, state.level))
            assertEquals(state.level.moveLimit, state.movesLeft)
        }
    }

    @Test
    fun nonAdjacentSwapIsRejectedWithoutStateMutation() {
        val engine = Match3Engine(Random(4))
        val state = engine.newGame(1)
        val turn = engine.swap(state, Cell(0, 0), Cell(2, 0))
        assertFalse(turn.accepted)
        assertEquals(state, turn.state)
    }

    @Test
    fun swapWithoutMatchIsRejectedWithoutSpendingMove() {
        val engine = Match3Engine(Random(12))
        val state = engine.newGame(3)
        var checked = false
        outer@ for (row in 0 until state.level.rows) {
            for (col in 0 until state.level.cols - 1) {
                val result = engine.swap(state, Cell(row, col), Cell(row, col + 1))
                if (!result.accepted) {
                    assertEquals(state.movesLeft, result.state.movesLeft)
                    checked = true
                    break@outer
                }
            }
        }
        assertTrue(checked)
    }

    @Test
    fun acceptedMoveAlwaysLeavesPlayableBoardOrFinishedState() {
        repeat(25) { seed ->
            val engine = Match3Engine(Random(seed + 100))
            var state = engine.newGame((seed % 10) + 1)
            var accepted = false
            outer@ for (row in 0 until state.level.rows) {
                for (col in 0 until state.level.cols) {
                    val neighbours = listOf(Cell(row + 1, col), Cell(row, col + 1))
                    for (next in neighbours) {
                        val turn = engine.swap(state, Cell(row, col), next)
                        if (turn.accepted) {
                            state = turn.state
                            accepted = true
                            assertTrue(state.finished || engine.hasPossibleMove(state.board, state.level))
                            assertTrue(turn.removed >= 3)
                            break@outer
                        }
                    }
                }
            }
            assertTrue(accepted)
        }
    }
}
