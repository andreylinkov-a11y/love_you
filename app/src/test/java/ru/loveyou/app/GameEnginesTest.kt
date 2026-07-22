package ru.loveyou.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEnginesTest {
    @Test
    fun match3BoardsStartWithoutMatchesAndHaveMove() {
        repeat(30) { seed ->
            val state = Match3Engine.create(seed = seed)
            assertTrue(Match3Engine.findMatches(state).isEmpty())
            assertTrue(Match3Engine.hasLegalMove(state))
        }
    }

    @Test
    fun invalidMatch3SwapDoesNotSpendMove() {
        val state = Match3Engine.create(seed = 41)
        val result = Match3Engine.swap(state, GridPoint(0, 0), GridPoint(2, 2), seed = 9)
        assertFalse(result.accepted)
        assertEquals(state, result.state)
    }

    @Test
    fun generatedMazesAlwaysReachGoal() {
        for (level in 1..30) {
            repeat(8) { seed ->
                val maze = MazeEngine.generate(level, seed + level * 100)
                val pathWithoutDoorRules = MazeEngine.shortestPath(maze.copy(doors = emptySet()))
                assertNotNull("level=$level seed=$seed", pathWithoutDoorRules)
                assertEquals(maze.start, pathWithoutDoorRules!!.first())
                assertEquals(maze.goal, pathWithoutDoorRules.last())
            }
        }
    }

    @Test
    fun slidingPuzzleShuffleIsReachableAndNotSolved() {
        for (size in 3..5) {
            repeat(20) { seed ->
                val board = SlidingPuzzleEngine.shuffled(size, 80, seed)
                assertEquals((0 until size * size).toSet(), board.toSet())
                assertFalse(board == SlidingPuzzleEngine.solved(size))
            }
        }
    }

    @Test
    fun rowSlideMovesWholeSegmentTowardBlank() {
        val board = listOf(1, 2, 3, 4, 5, 0, 6, 7, 8)
        val moved = SlidingPuzzleEngine.slide(board, 3, 3)
        assertEquals(listOf(1, 2, 3, 0, 4, 5, 6, 7, 8), moved)
    }

    @Test
    fun memoryDifficultyGrowsWithLevel() {
        val first = MemoryDifficulty.forLevel(1)
        val final = MemoryDifficulty.forLevel(12)
        assertTrue(final.rows * final.cols > first.rows * first.cols)
        assertTrue(final.previewMillis < first.previewMillis)
        assertNotNull(final.maxMistakes)
    }
}
