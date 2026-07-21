package ru.loveyou.app

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Match3EngineTest {
    @Test
    fun generatedBoardStartsCleanAndPlayable() {
        repeat(25) { level ->
            val state = Match3Engine.newGame(level + 1, seed = level * 31 + 7)
            assertTrue(Match3Engine.findMatches(state.board, state.level.size).isEmpty())
            assertTrue(Match3Engine.hasPossibleMove(state.board, state.level.size))
        }
    }

    @Test
    fun invalidSwapDoesNotConsumeMove() {
        val state = Match3Engine.newGame(1, seed = 42)
        val move = Match3Engine.playSwap(state, 0, state.level.size * state.level.size - 1, Random(1))
        assertFalse(move.accepted)
        assertEquals(state.movesLeft, move.state.movesLeft)
        assertEquals(state.board, move.state.board)
    }

    @Test
    fun horizontalAndVerticalMatchesAreDetected() {
        val H = Gem.HEART
        val F = Gem.FLOWER
        val S = Gem.STAR
        val G = Gem.GIFT
        val board = listOf(
            H, H, H, F,
            F, S, G, F,
            S, S, G, F,
            G, H, S, H
        )
        val matches = Match3Engine.findMatches(board, 4)
        assertTrue(0 in matches && 1 in matches && 2 in matches)
        assertTrue(3 in matches && 7 in matches && 11 in matches)
    }

    @Test
    fun levelDifficultyGrowsWithoutBecomingImpossible() {
        val first = Match3Level.forNumber(1)
        val tenth = Match3Level.forNumber(10)
        val thirtieth = Match3Level.forNumber(30)
        assertTrue(tenth.targetScore > first.targetScore)
        assertTrue(thirtieth.targetScore > tenth.targetScore)
        assertTrue(tenth.moves <= first.moves)
        assertTrue(thirtieth.moves >= 13)
    }
}
