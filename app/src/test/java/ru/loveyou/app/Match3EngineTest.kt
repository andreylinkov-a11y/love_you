package ru.loveyou.app

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Match3EngineTest {
    @Test
    fun generatedBoardStartsStableAndPlayable() {
        repeat(30) { seed ->
            val state = Match3Engine.newState(levelNumber = 8, seed = seed)
            assertTrue(Match3Engine.findMatches(state.board, state.level).isEmpty())
            assertTrue(Match3Engine.hasPossibleMove(state.board, state.level))
            assertEquals(state.level.rows * state.level.columns, state.board.size)
        }
    }

    @Test
    fun nonAdjacentSwapIsRejectedWithoutSpendingMove() {
        val state = Match3Engine.newState(1, seed = 42)
        val result = Match3Engine.swap(state, 0, state.level.columns + 1, Random(1))
        assertFalse(result.accepted)
        assertEquals(state, result.state)
    }

    @Test
    fun levelDifficultyRisesAndMovesAreBounded() {
        val first = Match3Engine.level(1)
        val tenth = Match3Engine.level(10)
        val thirtieth = Match3Engine.level(30)
        assertTrue(tenth.targetScore > first.targetScore)
        assertTrue(thirtieth.targetScore > tenth.targetScore)
        assertTrue(thirtieth.moves >= 12)
        assertTrue(tenth.rows >= first.rows)
    }

    @Test
    fun findMatchesDetectsHorizontalAndVerticalRuns() {
        val level = Match3Level(1, 3, 3, 10, 100, 5)
        val horizontal = listOf(
            Gem.HEART, Gem.HEART, Gem.HEART,
            Gem.FLOWER, Gem.STAR, Gem.GIFT,
            Gem.STAR, Gem.GIFT, Gem.FLOWER
        )
        assertEquals(setOf(0, 1, 2), Match3Engine.findMatches(horizontal, level))

        val vertical = listOf(
            Gem.MOON, Gem.FLOWER, Gem.STAR,
            Gem.MOON, Gem.STAR, Gem.GIFT,
            Gem.MOON, Gem.GIFT, Gem.FLOWER
        )
        assertEquals(setOf(0, 3, 6), Match3Engine.findMatches(vertical, level))
    }
}
