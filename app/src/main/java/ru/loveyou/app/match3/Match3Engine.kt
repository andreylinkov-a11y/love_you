package ru.loveyou.app.match3

import kotlin.random.Random

enum class Gem { HEART, STAR, FLOWER, GIFT, MOON, BERRY }

data class Cell(val row: Int, val col: Int)

data class Match3Level(
    val number: Int,
    val rows: Int = 8,
    val cols: Int = 7,
    val moveLimit: Int,
    val targetGem: Gem,
    val targetCount: Int,
    val gemKinds: Int
)

data class Match3State(
    val level: Match3Level,
    val board: List<Gem>,
    val movesLeft: Int,
    val collected: Int = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val finished: Boolean = false,
    val won: Boolean = false
) {
    fun index(cell: Cell): Int = cell.row * level.cols + cell.col
    fun gem(cell: Cell): Gem = board[index(cell)]
}

data class Match3Turn(
    val state: Match3State,
    val accepted: Boolean,
    val removed: Int = 0,
    val cascades: Int = 0
)

object Match3Levels {
    fun forNumber(number: Int): Match3Level {
        val safe = number.coerceAtLeast(1)
        val kinds = when {
            safe <= 3 -> 5
            else -> 6
        }
        return Match3Level(
            number = safe,
            moveLimit = (22 - safe / 2).coerceAtLeast(12),
            targetGem = Gem.entries[(safe - 1) % Gem.entries.size],
            targetCount = (12 + safe * 2).coerceAtMost(46),
            gemKinds = kinds
        )
    }
}

class Match3Engine(private val random: Random = Random.Default) {
    fun newGame(levelNumber: Int): Match3State {
        val level = Match3Levels.forNumber(levelNumber)
        var board: List<Gem>
        do {
            board = buildBoardWithoutInitialMatches(level)
        } while (!hasPossibleMove(board, level))
        return Match3State(level, board, level.moveLimit)
    }

    fun swap(state: Match3State, first: Cell, second: Cell): Match3Turn {
        if (state.finished || !inside(first, state.level) || !inside(second, state.level)) {
            return Match3Turn(state, accepted = false)
        }
        val distance = kotlin.math.abs(first.row - second.row) + kotlin.math.abs(first.col - second.col)
        if (distance != 1) return Match3Turn(state, accepted = false)

        val mutable = state.board.toMutableList()
        val a = state.index(first)
        val b = state.index(second)
        mutable[a] = state.board[b]
        mutable[b] = state.board[a]
        if (findMatches(mutable, state.level).isEmpty()) return Match3Turn(state, accepted = false)

        var board = mutable.toList()
        var totalRemoved = 0
        var targetRemoved = 0
        var cascades = 0
        while (true) {
            val matches = findMatches(board, state.level)
            if (matches.isEmpty()) break
            cascades++
            totalRemoved += matches.size
            targetRemoved += matches.count { board[it] == state.level.targetGem }
            board = collapseAndRefill(board, matches, state.level)
        }
        if (!hasPossibleMove(board, state.level)) board = reshuffle(board, state.level)

        val remainingMoves = state.movesLeft - 1
        val collected = state.collected + targetRemoved
        val won = collected >= state.level.targetCount
        val finished = won || remainingMoves <= 0
        val scoreGain = totalRemoved * 10 * cascades.coerceAtLeast(1)
        val next = state.copy(
            board = board,
            movesLeft = remainingMoves,
            collected = collected,
            score = state.score + scoreGain,
            combo = cascades,
            finished = finished,
            won = won
        )
        return Match3Turn(next, accepted = true, removed = totalRemoved, cascades = cascades)
    }

    fun findMatches(board: List<Gem>, level: Match3Level): Set<Int> {
        val result = mutableSetOf<Int>()
        for (row in 0 until level.rows) {
            var start = 0
            while (start < level.cols) {
                var end = start + 1
                val gem = board[row * level.cols + start]
                while (end < level.cols && board[row * level.cols + end] == gem) end++
                if (end - start >= 3) for (col in start until end) result += row * level.cols + col
                start = end
            }
        }
        for (col in 0 until level.cols) {
            var start = 0
            while (start < level.rows) {
                var end = start + 1
                val gem = board[start * level.cols + col]
                while (end < level.rows && board[end * level.cols + col] == gem) end++
                if (end - start >= 3) for (row in start until end) result += row * level.cols + col
                start = end
            }
        }
        return result
    }

    fun hasPossibleMove(board: List<Gem>, level: Match3Level): Boolean {
        for (row in 0 until level.rows) for (col in 0 until level.cols) {
            val current = Cell(row, col)
            val neighbours = listOf(Cell(row + 1, col), Cell(row, col + 1))
            for (next in neighbours) {
                if (!inside(next, level)) continue
                val copy = board.toMutableList()
                val a = row * level.cols + col
                val b = next.row * level.cols + next.col
                val temp = copy[a]; copy[a] = copy[b]; copy[b] = temp
                if (findMatches(copy, level).isNotEmpty()) return true
            }
        }
        return false
    }

    private fun buildBoardWithoutInitialMatches(level: Match3Level): List<Gem> {
        val gems = Gem.entries.take(level.gemKinds)
        val board = MutableList(level.rows * level.cols) { gems.first() }
        for (row in 0 until level.rows) for (col in 0 until level.cols) {
            val forbidden = mutableSetOf<Gem>()
            if (col >= 2 && board[row * level.cols + col - 1] == board[row * level.cols + col - 2]) forbidden += board[row * level.cols + col - 1]
            if (row >= 2 && board[(row - 1) * level.cols + col] == board[(row - 2) * level.cols + col]) forbidden += board[(row - 1) * level.cols + col]
            board[row * level.cols + col] = gems.filterNot { it in forbidden }.random(random)
        }
        return board
    }

    private fun collapseAndRefill(board: List<Gem>, removed: Set<Int>, level: Match3Level): List<Gem> {
        val gems = Gem.entries.take(level.gemKinds)
        val next = MutableList(level.rows * level.cols) { gems.random(random) }
        for (col in 0 until level.cols) {
            val survivors = (level.rows - 1 downTo 0)
                .map { it * level.cols + col }
                .filterNot { it in removed }
                .map { board[it] }
            var targetRow = level.rows - 1
            survivors.forEach { gem -> next[targetRow-- * level.cols + col] = gem }
            while (targetRow >= 0) next[targetRow-- * level.cols + col] = gems.random(random)
        }
        return next
    }

    private fun reshuffle(board: List<Gem>, level: Match3Level): List<Gem> {
        repeat(100) {
            val shuffled = board.shuffled(random)
            if (findMatches(shuffled, level).isEmpty() && hasPossibleMove(shuffled, level)) return shuffled
        }
        return buildBoardWithoutInitialMatches(level).let { if (hasPossibleMove(it, level)) it else reshuffle(it, level) }
    }

    private fun inside(cell: Cell, level: Match3Level): Boolean =
        cell.row in 0 until level.rows && cell.col in 0 until level.cols
}
