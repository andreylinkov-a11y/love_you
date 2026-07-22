package ru.loveyou.app

import org.junit.Assert.*
import org.junit.Test

class RescueEngineTest {
    @Test fun generatedMazesAreReachable() {
        for (level in 1..30) for (seed in 1..12) {
            val maze = RescueEngine.generate(level, seed)
            assertTrue("level=$level seed=$seed", RescueEngine.reachable(maze))
            assertTrue(maze.start in maze.open)
            assertTrue(maze.goal in maze.open)
        }
    }

    @Test fun storedShortestPathIsContinuous() {
        val maze = RescueEngine.generate(18, 42)
        assertEquals(maze.start, maze.shortestPath.first())
        assertEquals(maze.goal, maze.shortestPath.last())
        maze.shortestPath.zipWithNext().forEach { (a, b) -> assertTrue(maze.canMove(a, b)) }
    }

    @Test fun difficultyGrowsButIsBounded() {
        assertEquals(7, RescueEngine.generate(1).rows)
        assertTrue(RescueEngine.generate(10).rows > RescueEngine.generate(1).rows)
        assertEquals(15, RescueEngine.generate(100).rows)
    }
}
