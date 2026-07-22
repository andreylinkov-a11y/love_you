package ru.loveyou.app.game.match3

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class Match3EngineTest {
    @Test
    fun generatedBoardStartsWithoutMatchesAndHasMove() {
        val engine = Match3Engine(random = Random(42))
        val board = engine.createPlayableBoard()

        assertTrue(engine.findMatches(board).isEmpty())
        assertTrue(engine.hasPossibleMove(board))
    }

    @Test
    fun nonAdjacentSwapIsRejected() {
        val engine = Match3Engine(random = Random(1))
        val board = engine.createPlayableBoard()

        val result = engine.swap(board, Cell(0, 0), Cell(2, 0))

        assertFalse(result.accepted)
        assertEquals(board, result.board)
    }

    @Test
    fun swapWithoutMatchIsRejectedAndBoardIsUnchanged() {
        val engine = Match3Engine(random = Random(7))
        val board = boardOf(
            "HFSG",
            "SGHF",
            "FHSG",
            "GSFH"
        )

        val result = engine.swap(board, Cell(0, 0), Cell(0, 1))

        assertFalse(result.accepted)
        assertEquals(board, result.board)
    }

    @Test
    fun validSwapCreatesCascadeAndScore() {
        val engine = Match3Engine(rows = 4, columns = 4, random = Random(5))
        val board = boardOf(
            "HHFS",
            "FHSB",
            "SGHB",
            "BFSH"
        )

        val result = engine.swap(board, Cell(0, 2), Cell(1, 2))

        assertTrue(result.accepted)
        assertTrue(result.cascades.isNotEmpty())
        assertTrue(result.totalScore >= 30)
        assertTrue(engine.findMatches(result.board).isEmpty())
    }

    private fun boardOf(vararg rows: String): Match3Board {
        val mapping = mapOf(
            'H' to GemType.HEART,
            'F' to GemType.FLOWER,
            'S' to GemType.STAR,
            'B' to GemType.BERRY,
            'U' to GemType.SUN,
            'G' to GemType.GIFT
        )
        return Match3Board(
            rows = rows.size,
            columns = rows.first().length,
            cells = rows.flatMap { row -> row.map { Gem(mapping.getValue(it)) } }
        )
    }
}
