package ru.loveyou.app

import kotlin.math.abs
import kotlin.random.Random

data class GridPoint(val row: Int, val col: Int)

enum class MatchGem { HEART, FLOWER, STAR, GIFT, MOON, BERRY }

data class Match3State(
    val rows: Int,
    val cols: Int,
    val cells: List<MatchGem>,
    val score: Int = 0,
    val movesLeft: Int = 20,
    val cascade: Int = 0
) {
    operator fun get(point: GridPoint): MatchGem = cells[point.row * cols + point.col]
    fun index(point: GridPoint): Int = point.row * cols + point.col
    fun inBounds(point: GridPoint): Boolean = point.row in 0 until rows && point.col in 0 until cols
}

data class Match3Turn(
    val state: Match3State,
    val accepted: Boolean,
    val removed: Int,
    val cascades: Int
)

object Match3Engine {
    fun create(rows: Int = 8, cols: Int = 7, moves: Int = 20, seed: Int = 1): Match3State {
        require(rows >= 4 && cols >= 4)
        val random = Random(seed)
        val cells = MutableList(rows * cols) { MatchGem.HEART }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val forbidden = buildSet {
                    if (col >= 2 && cells[row * cols + col - 1] == cells[row * cols + col - 2]) add(cells[row * cols + col - 1])
                    if (row >= 2 && cells[(row - 1) * cols + col] == cells[(row - 2) * cols + col]) add(cells[(row - 1) * cols + col])
                }
                cells[row * cols + col] = MatchGem.entries.filterNot { it in forbidden }.random(random)
            }
        }
        var state = Match3State(rows, cols, cells, movesLeft = moves)
        var attempts = 0
        while (!hasLegalMove(state) && attempts++ < 40) state = create(rows, cols, moves, seed + attempts * 31)
        return state
    }

    fun swap(state: Match3State, first: GridPoint, second: GridPoint, seed: Int): Match3Turn {
        if (!state.inBounds(first) || !state.inBounds(second) || state.movesLeft <= 0) return Match3Turn(state, false, 0, 0)
        if (abs(first.row - second.row) + abs(first.col - second.col) != 1) return Match3Turn(state, false, 0, 0)
        val cells = state.cells.toMutableList()
        val a = state.index(first)
        val b = state.index(second)
        cells[a] = state.cells[b]
        cells[b] = state.cells[a]
        var working = state.copy(cells = cells)
        if (findMatches(working).isEmpty()) return Match3Turn(state, false, 0, 0)

        val random = Random(seed)
        var totalRemoved = 0
        var cascades = 0
        while (true) {
            val matches = findMatches(working)
            if (matches.isEmpty()) break
            cascades++
            totalRemoved += matches.size
            working = collapse(working, matches, random)
        }
        val gained = totalRemoved * 10 + (1 until cascades).sumOf { it * 20 }
        working = working.copy(score = state.score + gained, movesLeft = state.movesLeft - 1, cascade = cascades)
        if (!hasLegalMove(working) && working.movesLeft > 0) working = reshuffle(working, seed + 997)
        return Match3Turn(working, true, totalRemoved, cascades)
    }

    fun findMatches(state: Match3State): Set<GridPoint> {
        val found = mutableSetOf<GridPoint>()
        for (row in 0 until state.rows) {
            var start = 0
            while (start < state.cols) {
                var end = start + 1
                while (end < state.cols && state[GridPoint(row, end)] == state[GridPoint(row, start)]) end++
                if (end - start >= 3) for (col in start until end) found += GridPoint(row, col)
                start = end
            }
        }
        for (col in 0 until state.cols) {
            var start = 0
            while (start < state.rows) {
                var end = start + 1
                while (end < state.rows && state[GridPoint(end, col)] == state[GridPoint(start, col)]) end++
                if (end - start >= 3) for (row in start until end) found += GridPoint(row, col)
                start = end
            }
        }
        return found
    }

    fun hasLegalMove(state: Match3State): Boolean {
        for (row in 0 until state.rows) for (col in 0 until state.cols) {
            val p = GridPoint(row, col)
            for (q in listOf(GridPoint(row + 1, col), GridPoint(row, col + 1))) {
                if (!state.inBounds(q)) continue
                val cells = state.cells.toMutableList()
                val a = state.index(p); val b = state.index(q)
                val tmp = cells[a]; cells[a] = cells[b]; cells[b] = tmp
                if (findMatches(state.copy(cells = cells)).isNotEmpty()) return true
            }
        }
        return false
    }

    private fun collapse(state: Match3State, removed: Set<GridPoint>, random: Random): Match3State {
        val next = MutableList(state.rows * state.cols) { MatchGem.HEART }
        for (col in 0 until state.cols) {
            val survivors = (state.rows - 1 downTo 0)
                .map { row -> GridPoint(row, col) }
                .filterNot { it in removed }
                .map { state[it] }
                .toMutableList()
            while (survivors.size < state.rows) survivors += MatchGem.entries.random(random)
            for (row in state.rows - 1 downTo 0) next[row * state.cols + col] = survivors[state.rows - 1 - row]
        }
        return state.copy(cells = next)
    }

    private fun reshuffle(state: Match3State, seed: Int): Match3State {
        val random = Random(seed)
        repeat(100) {
            val shuffled = state.cells.shuffled(random)
            val candidate = state.copy(cells = shuffled)
            if (findMatches(candidate).isEmpty() && hasLegalMove(candidate)) return candidate
        }
        return create(state.rows, state.cols, state.movesLeft, seed).copy(score = state.score)
    }
}

data class MazeState(
    val size: Int,
    val open: Set<GridPoint>,
    val start: GridPoint,
    val goal: GridPoint,
    val keys: Set<GridPoint> = emptySet(),
    val doors: Set<GridPoint> = emptySet()
)

object MazeEngine {
    fun generate(level: Int, seed: Int): MazeState {
        val size = (7 + level / 3 * 2).coerceIn(7, 15).let { if (it % 2 == 0) it + 1 else it }
        val random = Random(seed)
        val start = GridPoint(size - 1, 0)
        val goal = GridPoint(0, size - 1)
        val open = mutableSetOf(start)
        var current = start
        while (current != goal) {
            val choices = mutableListOf<GridPoint>()
            if (current.row > goal.row) choices += GridPoint(current.row - 1, current.col)
            if (current.col < goal.col) choices += GridPoint(current.row, current.col + 1)
            current = choices.random(random)
            open += current
        }
        val branchTarget = (size * size * (.20f + level.coerceAtMost(12) * .015f)).toInt()
        repeat(branchTarget) {
            val base = open.random(random)
            val candidates = listOf(
                GridPoint(base.row - 1, base.col), GridPoint(base.row + 1, base.col),
                GridPoint(base.row, base.col - 1), GridPoint(base.row, base.col + 1)
            ).filter { it.row in 0 until size && it.col in 0 until size }
            if (candidates.isNotEmpty()) open += candidates.random(random)
        }
        val path = shortestPath(MazeState(size, open, start, goal)) ?: listOf(start, goal)
        val useDoor = level >= 4 && path.size > 7
        val door = if (useDoor) path[path.size * 2 / 3] else null
        val keyCandidates = path.take(path.size / 2).drop(1)
        val key = if (useDoor && keyCandidates.isNotEmpty()) keyCandidates.random(random) else null
        return MazeState(size, open, start, goal, listOfNotNull(key).toSet(), listOfNotNull(door).toSet())
    }

    fun shortestPath(state: MazeState, collectedKeys: Set<GridPoint> = emptySet()): List<GridPoint>? {
        val queue = ArrayDeque<GridPoint>()
        val previous = mutableMapOf<GridPoint, GridPoint?>()
        queue += state.start
        previous[state.start] = null
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            if (p == state.goal) break
            val neighbors = listOf(GridPoint(p.row - 1, p.col), GridPoint(p.row + 1, p.col), GridPoint(p.row, p.col - 1), GridPoint(p.row, p.col + 1))
            for (n in neighbors) {
                if (n !in state.open || n in previous) continue
                if (n in state.doors && collectedKeys.isEmpty() && state.keys.isNotEmpty()) continue
                previous[n] = p
                queue += n
            }
        }
        if (state.goal !in previous) return null
        return buildList {
            var p: GridPoint? = state.goal
            while (p != null) { add(p); p = previous[p] }
        }.reversed()
    }
}

data class MemoryLevel(val rows: Int, val cols: Int, val previewMillis: Long, val maxMistakes: Int?)

object MemoryDifficulty {
    fun forLevel(level: Int): MemoryLevel = when {
        level <= 2 -> MemoryLevel(3, 4, 1800, null)
        level <= 5 -> MemoryLevel(4, 4, 1400, 10)
        level <= 8 -> MemoryLevel(4, 5, 1100, 8)
        else -> MemoryLevel(5, 6, 850, 6)
    }
}

object SlidingPuzzleEngine {
    fun solved(size: Int): List<Int> = (1 until size * size).toList() + 0

    fun shuffled(size: Int, moves: Int, seed: Int): List<Int> {
        require(size in 3..5)
        val random = Random(seed)
        var board = solved(size)
        var previousBlank = -1
        repeat(moves.coerceAtLeast(size * size * 4)) {
            val blank = board.indexOf(0)
            val row = blank / size; val col = blank % size
            val candidates = buildList {
                if (row > 0) add(blank - size)
                if (row < size - 1) add(blank + size)
                if (col > 0) add(blank - 1)
                if (col < size - 1) add(blank + 1)
            }.filter { it != previousBlank }
            val tile = candidates.random(random)
            previousBlank = blank
            board = board.toMutableList().also { it[blank] = it[tile]; it[tile] = 0 }
        }
        return board
    }

    fun slide(board: List<Int>, size: Int, index: Int): List<Int> {
        if (index !in board.indices || board[index] == 0) return board
        val blank = board.indexOf(0)
        val sameRow = index / size == blank / size
        val sameCol = index % size == blank % size
        if (!sameRow && !sameCol) return board
        val next = board.toMutableList()
        if (sameRow) {
            if (index < blank) for (i in blank downTo index + 1) next[i] = next[i - 1]
            else for (i in blank until index) next[i] = next[i + 1]
        } else {
            if (index < blank) for (i in blank downTo index + size step size) next[i] = next[i - size]
            else for (i in blank until index step size) next[i] = next[i + size]
        }
        next[index] = 0
        return next
    }
}
