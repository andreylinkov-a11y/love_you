package ru.loveyou.app

import kotlin.math.abs
import kotlin.random.Random

enum class Gem(val emoji: String) {
    HEART("❤️"), FLOWER("🌸"), STAR("⭐"), GIFT("🎁"), BERRY("🍓"), MOON("🌙")
}

data class Match3Level(
    val number: Int,
    val rows: Int,
    val columns: Int,
    val moves: Int,
    val targetScore: Int,
    val gemTypes: Int
)

data class Match3State(
    val level: Match3Level,
    val board: List<Gem>,
    val movesLeft: Int,
    val score: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val finished: Boolean = false,
    val won: Boolean = false
)

data class Match3MoveResult(
    val state: Match3State,
    val accepted: Boolean,
    val removed: Int = 0,
    val cascades: Int = 0
)

object Match3Engine {
    fun level(number: Int): Match3Level {
        val safe = number.coerceAtLeast(1)
        val rows = if (safe < 6) 6 else 7
        val columns = if (safe < 11) 6 else 7
        val gemTypes = when {
            safe < 4 -> 5
            else -> 6
        }
        val moves = (22 - safe / 2).coerceAtLeast(12)
        val target = 650 + safe * 210 + safe * safe * 12
        return Match3Level(safe, rows, columns, moves, target, gemTypes)
    }

    fun newState(levelNumber: Int, seed: Int = Random.nextInt()): Match3State {
        val level = level(levelNumber)
        var attempt = 0
        var board: List<Gem>
        do {
            board = generateBoard(level, Random(seed + attempt))
            attempt++
        } while ((findMatches(board, level).isNotEmpty() || !hasPossibleMove(board, level)) && attempt < 300)
        return Match3State(level, board, level.moves)
    }

    fun swap(state: Match3State, first: Int, second: Int, random: Random = Random.Default): Match3MoveResult {
        if (state.finished || state.movesLeft <= 0) return Match3MoveResult(state, false)
        if (!areAdjacent(first, second, state.level) || first !in state.board.indices || second !in state.board.indices) {
            return Match3MoveResult(state, false)
        }
        val swapped = state.board.toMutableList().also {
            val tmp = it[first]
            it[first] = it[second]
            it[second] = tmp
        }
        if (findMatches(swapped, state.level).isEmpty()) return Match3MoveResult(state, false)

        var board = swapped.toList()
        var cascade = 0
        var removedTotal = 0
        var gained = 0
        while (true) {
            val matches = findMatches(board, state.level)
            if (matches.isEmpty()) break
            cascade++
            removedTotal += matches.size
            gained += matches.size * 25 * cascade
            board = collapse(board, state.level, matches, random)
        }

        if (!hasPossibleMove(board, state.level)) board = reshuffle(board, state.level, random)
        val movesLeft = state.movesLeft - 1
        val score = state.score + gained
        val won = score >= state.level.targetScore
        val finished = won || movesLeft <= 0
        val combo = cascade
        return Match3MoveResult(
            state.copy(
                board = board,
                movesLeft = movesLeft,
                score = score,
                combo = combo,
                bestCombo = maxOf(state.bestCombo, combo),
                won = won,
                finished = finished
            ),
            accepted = true,
            removed = removedTotal,
            cascades = cascade
        )
    }

    fun findMatches(board: List<Gem>, level: Match3Level): Set<Int> {
        val result = mutableSetOf<Int>()
        for (r in 0 until level.rows) {
            var start = 0
            while (start < level.columns) {
                var end = start + 1
                while (end < level.columns && board[index(r, end, level)] == board[index(r, start, level)]) end++
                if (end - start >= 3) for (c in start until end) result += index(r, c, level)
                start = end
            }
        }
        for (c in 0 until level.columns) {
            var start = 0
            while (start < level.rows) {
                var end = start + 1
                while (end < level.rows && board[index(end, c, level)] == board[index(start, c, level)]) end++
                if (end - start >= 3) for (r in start until end) result += index(r, c, level)
                start = end
            }
        }
        return result
    }

    fun hasPossibleMove(board: List<Gem>, level: Match3Level): Boolean {
        for (i in board.indices) {
            val row = i / level.columns
            val col = i % level.columns
            if (col + 1 < level.columns && createsMatchAfterSwap(board, level, i, i + 1)) return true
            if (row + 1 < level.rows && createsMatchAfterSwap(board, level, i, i + level.columns)) return true
        }
        return false
    }

    private fun generateBoard(level: Match3Level, random: Random): List<Gem> {
        val gems = Gem.entries.take(level.gemTypes)
        val board = MutableList(level.rows * level.columns) { Gem.HEART }
        for (r in 0 until level.rows) {
            for (c in 0 until level.columns) {
                val forbidden = mutableSetOf<Gem>()
                if (c >= 2 && board[index(r, c - 1, level)] == board[index(r, c - 2, level)]) forbidden += board[index(r, c - 1, level)]
                if (r >= 2 && board[index(r - 1, c, level)] == board[index(r - 2, c, level)]) forbidden += board[index(r - 1, c, level)]
                board[index(r, c, level)] = gems.filterNot { it in forbidden }.random(random)
            }
        }
        return board
    }

    private fun createsMatchAfterSwap(board: List<Gem>, level: Match3Level, a: Int, b: Int): Boolean {
        val copy = board.toMutableList()
        val tmp = copy[a]
        copy[a] = copy[b]
        copy[b] = tmp
        return findMatches(copy, level).isNotEmpty()
    }

    private fun collapse(board: List<Gem>, level: Match3Level, removed: Set<Int>, random: Random): List<Gem> {
        val gems = Gem.entries.take(level.gemTypes)
        val result = MutableList(board.size) { Gem.HEART }
        for (c in 0 until level.columns) {
            val remaining = (level.rows - 1 downTo 0)
                .map { board[index(it, c, level)] to index(it, c, level) }
                .filterNot { it.second in removed }
                .map { it.first }
            var writeRow = level.rows - 1
            for (gem in remaining) result[index(writeRow--, c, level)] = gem
            while (writeRow >= 0) result[index(writeRow--, c, level)] = gems.random(random)
        }
        return result
    }

    private fun reshuffle(board: List<Gem>, level: Match3Level, random: Random): List<Gem> {
        repeat(100) {
            val candidate = board.shuffled(random)
            if (findMatches(candidate, level).isEmpty() && hasPossibleMove(candidate, level)) return candidate
        }
        return generateBoard(level, random)
    }

    private fun areAdjacent(a: Int, b: Int, level: Match3Level): Boolean {
        val ar = a / level.columns
        val ac = a % level.columns
        val br = b / level.columns
        val bc = b % level.columns
        return abs(ar - br) + abs(ac - bc) == 1
    }

    private fun index(row: Int, column: Int, level: Match3Level) = row * level.columns + column
}
