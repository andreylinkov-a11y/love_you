package ru.loveyou.app

import kotlin.math.abs
import kotlin.random.Random

data class PuzzleLevel(
    val number: Int,
    val size: Int,
    val shuffleMoves: Int,
    val parMoves: Int
)

data class PuzzleState(
    val level: PuzzleLevel,
    val board: List<Int>,
    val moves: Int = 0,
    val finished: Boolean = false
)

data class PuzzleMoveResult(
    val state: PuzzleState,
    val accepted: Boolean,
    val shiftedTiles: Int = 0
)

object SlidingPuzzleEngine {
    fun level(number: Int): PuzzleLevel {
        val n = number.coerceAtLeast(1)
        val size = if (n <= 5) 3 else 4
        val shuffle = if (size == 3) 24 + n * 6 else 70 + (n - 5) * 10
        val par = if (size == 3) 18 + n * 3 else 48 + (n - 5) * 6
        return PuzzleLevel(n, size, shuffle, par)
    }

    fun solvedBoard(size: Int): List<Int> = (1 until size * size).toList() + 0

    fun newState(levelNumber: Int, seed: Int = Random.nextInt()): PuzzleState {
        val level = level(levelNumber)
        var board = solvedBoard(level.size)
        val random = Random(seed)
        var previousBlank = -1
        repeat(level.shuffleMoves) {
            val blank = board.indexOf(0)
            val candidates = adjacent(blank, level.size).filter { it != previousBlank }
            val next = (if (candidates.isEmpty()) adjacent(blank, level.size) else candidates).random(random)
            previousBlank = blank
            board = swap(board, blank, next)
        }
        if (isSolved(board)) {
            val blank = board.indexOf(0)
            board = swap(board, blank, adjacent(blank, level.size).first())
        }
        return PuzzleState(level, board)
    }

    fun move(state: PuzzleState, tileIndex: Int): PuzzleMoveResult {
        if (state.finished || tileIndex !in state.board.indices || state.board[tileIndex] == 0) return PuzzleMoveResult(state, false)
        val size = state.level.size
        val blank = state.board.indexOf(0)
        val tileRow = tileIndex / size
        val tileCol = tileIndex % size
        val blankRow = blank / size
        val blankCol = blank % size
        if (tileRow != blankRow && tileCol != blankCol) return PuzzleMoveResult(state, false)

        val mutable = state.board.toMutableList()
        var shifted = 0
        if (tileRow == blankRow) {
            if (tileIndex < blank) {
                for (i in blank downTo tileIndex + 1) { mutable[i] = mutable[i - 1]; shifted++ }
            } else {
                for (i in blank until tileIndex) { mutable[i] = mutable[i + 1]; shifted++ }
            }
        } else {
            if (tileIndex < blank) {
                var i = blank
                while (i > tileIndex) { mutable[i] = mutable[i - size]; i -= size; shifted++ }
            } else {
                var i = blank
                while (i < tileIndex) { mutable[i] = mutable[i + size]; i += size; shifted++ }
            }
        }
        mutable[tileIndex] = 0
        val board = mutable.toList()
        return PuzzleMoveResult(
            state.copy(board = board, moves = state.moves + 1, finished = isSolved(board)),
            accepted = true,
            shiftedTiles = shifted
        )
    }

    fun targetIndex(value: Int, size: Int): Int = if (value == 0) size * size - 1 else value - 1
    fun isSolved(board: List<Int>): Boolean = board == solvedBoard(kotlin.math.sqrt(board.size.toDouble()).toInt())
    fun manhattanDistance(board: List<Int>, size: Int): Int = board.withIndex().sumOf { (index, value) ->
        if (value == 0) 0 else {
            val target = targetIndex(value, size)
            abs(index / size - target / size) + abs(index % size - target % size)
        }
    }

    private fun adjacent(index: Int, size: Int): List<Int> {
        val row = index / size
        val col = index % size
        return buildList {
            if (row > 0) add(index - size)
            if (row < size - 1) add(index + size)
            if (col > 0) add(index - 1)
            if (col < size - 1) add(index + 1)
        }
    }

    private fun swap(board: List<Int>, a: Int, b: Int): List<Int> = board.toMutableList().also {
        val tmp = it[a]; it[a] = it[b]; it[b] = tmp
    }
}
