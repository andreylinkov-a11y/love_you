package ru.loveyou.app

import kotlin.math.abs
import kotlin.random.Random

enum class Gem(val emoji: String) {
    HEART("❤️"), FLOWER("🌸"), STAR("⭐"), GIFT("🎁"), BERRY("🍓"), MOON("🌙")
}

data class Match3Level(
    val number: Int,
    val size: Int,
    val moves: Int,
    val targetScore: Int,
    val targetGem: Gem?,
    val targetCount: Int
) {
    companion object {
        fun forNumber(level: Int): Match3Level {
            val safe = level.coerceAtLeast(1)
            val size = if (safe < 6) 6 else 7
            val moves = (22 - safe / 2).coerceAtLeast(13)
            val target = Gem.entries[(safe - 1) % Gem.entries.size]
            return Match3Level(
                number = safe,
                size = size,
                moves = moves,
                targetScore = 650 + safe * 210,
                targetGem = if (safe >= 3) target else null,
                targetCount = if (safe >= 3) 8 + safe * 2 else 0
            )
        }
    }
}

data class Match3State(
    val level: Match3Level,
    val board: List<Gem>,
    val movesLeft: Int,
    val score: Int = 0,
    val collected: Int = 0,
    val bestCascade: Int = 0,
    val lastMessage: String = "Собери три одинаковых символа"
) {
    val won: Boolean
        get() = score >= level.targetScore &&
            (level.targetGem == null || collected >= level.targetCount)
    val lost: Boolean get() = movesLeft <= 0 && !won
}

data class Match3Move(
    val state: Match3State,
    val accepted: Boolean,
    val removed: Int,
    val cascades: Int
)

object Match3Engine {
    fun newGame(levelNumber: Int, seed: Int = levelNumber * 7919): Match3State {
        val level = Match3Level.forNumber(levelNumber)
        val random = Random(seed)
        val board = generatePlayableBoard(level.size, random)
        return Match3State(level = level, board = board, movesLeft = level.moves)
    }

    fun playSwap(state: Match3State, first: Int, second: Int, random: Random = Random.Default): Match3Move {
        if (state.won || state.lost || !areAdjacent(first, second, state.level.size)) {
            return Match3Move(state.copy(lastMessage = "Выбери соседние фишки"), false, 0, 0)
        }
        val swapped = state.board.toMutableList().also {
            val temp = it[first]
            it[first] = it[second]
            it[second] = temp
        }
        if (findMatches(swapped, state.level.size).isEmpty()) {
            return Match3Move(state.copy(lastMessage = "Этот ход не создаёт комбинацию"), false, 0, 0)
        }

        var board = swapped.toList()
        var cascades = 0
        var removedTotal = 0
        var collected = state.collected
        var gained = 0
        while (true) {
            val matches = findMatches(board, state.level.size)
            if (matches.isEmpty()) break
            cascades++
            removedTotal += matches.size
            if (state.level.targetGem != null) {
                collected += matches.count { board[it] == state.level.targetGem }
            }
            gained += matches.size * 12 * cascades
            board = collapse(board, matches, state.level.size, random)
        }
        if (!hasPossibleMove(board, state.level.size)) {
            board = generatePlayableBoard(state.level.size, random)
        }
        val next = state.copy(
            board = board,
            movesLeft = state.movesLeft - 1,
            score = state.score + gained,
            collected = collected,
            bestCascade = maxOf(state.bestCascade, cascades),
            lastMessage = when {
                cascades >= 4 -> "Невероятный каскад ×$cascades!"
                cascades == 3 -> "Волшебный каскад ×3"
                cascades == 2 -> "Комбо ×2"
                removedTotal >= 5 -> "Потрясающая комбинация!"
                else -> "+$gained очков"
            }
        )
        return Match3Move(next, true, removedTotal, cascades)
    }

    fun findMatches(board: List<Gem>, size: Int): Set<Int> {
        val result = mutableSetOf<Int>()
        for (row in 0 until size) {
            var start = 0
            while (start < size) {
                var end = start + 1
                while (end < size && board[row * size + end] == board[row * size + start]) end++
                if (end - start >= 3) for (column in start until end) result += row * size + column
                start = end
            }
        }
        for (column in 0 until size) {
            var start = 0
            while (start < size) {
                var end = start + 1
                while (end < size && board[end * size + column] == board[start * size + column]) end++
                if (end - start >= 3) for (row in start until end) result += row * size + column
                start = end
            }
        }
        return result
    }

    fun hasPossibleMove(board: List<Gem>, size: Int): Boolean {
        for (index in board.indices) {
            val row = index / size
            val column = index % size
            if (column + 1 < size && createsMatchAfterSwap(board, size, index, index + 1)) return true
            if (row + 1 < size && createsMatchAfterSwap(board, size, index, index + size)) return true
        }
        return false
    }

    private fun createsMatchAfterSwap(board: List<Gem>, size: Int, first: Int, second: Int): Boolean {
        val copy = board.toMutableList()
        val temp = copy[first]
        copy[first] = copy[second]
        copy[second] = temp
        return findMatches(copy, size).isNotEmpty()
    }

    private fun areAdjacent(first: Int, second: Int, size: Int): Boolean {
        if (first !in 0 until size * size || second !in 0 until size * size) return false
        val rowDistance = abs(first / size - second / size)
        val columnDistance = abs(first % size - second % size)
        return rowDistance + columnDistance == 1
    }

    private fun collapse(board: List<Gem>, removed: Set<Int>, size: Int, random: Random): List<Gem> {
        val result = MutableList(size * size) { Gem.HEART }
        for (column in 0 until size) {
            val survivors = (size - 1 downTo 0)
                .map { row -> row * size + column }
                .filter { it !in removed }
                .map { board[it] }
                .toMutableList()
            while (survivors.size < size) survivors += Gem.entries[random.nextInt(Gem.entries.size)]
            for (row in size - 1 downTo 0) {
                result[row * size + column] = survivors[size - 1 - row]
            }
        }
        return result
    }

    private fun generatePlayableBoard(size: Int, random: Random): List<Gem> {
        repeat(200) {
            val board = MutableList(size * size) { Gem.HEART }
            for (index in board.indices) {
                val row = index / size
                val column = index % size
                val forbidden = mutableSetOf<Gem>()
                if (column >= 2 && board[index - 1] == board[index - 2]) forbidden += board[index - 1]
                if (row >= 2 && board[index - size] == board[index - size * 2]) forbidden += board[index - size]
                val choices = Gem.entries.filterNot { it in forbidden }
                board[index] = choices[random.nextInt(choices.size)]
            }
            if (findMatches(board, size).isEmpty() && hasPossibleMove(board, size)) return board
        }
        error("Unable to generate a playable match-3 board")
    }
}
