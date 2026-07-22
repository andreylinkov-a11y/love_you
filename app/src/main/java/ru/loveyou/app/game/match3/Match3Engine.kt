package ru.loveyou.app.game.match3

import kotlin.math.abs
import kotlin.random.Random

enum class GemType { HEART, FLOWER, STAR, BERRY, SUN, GIFT }
enum class SpecialGem { NONE, ROW, COLUMN, BOMB, RAINBOW }

data class Gem(
    val type: GemType,
    val special: SpecialGem = SpecialGem.NONE
)

data class Cell(val row: Int, val column: Int)

data class MatchGroup(
    val cells: Set<Cell>,
    val type: GemType
)

data class CascadeStep(
    val removed: Set<Cell>,
    val score: Int,
    val combo: Int
)

data class SwapResult(
    val accepted: Boolean,
    val board: Match3Board,
    val cascades: List<CascadeStep> = emptyList(),
    val totalScore: Int = 0
)

data class Match3Board(
    val rows: Int,
    val columns: Int,
    val cells: List<Gem?>
) {
    init {
        require(rows >= 3 && columns >= 3)
        require(cells.size == rows * columns)
    }

    operator fun get(cell: Cell): Gem? = cells[index(cell)]

    fun with(cell: Cell, gem: Gem?): Match3Board = copy(
        cells = cells.toMutableList().also { it[index(cell)] = gem }
    )

    fun index(cell: Cell): Int {
        require(cell.row in 0 until rows && cell.column in 0 until columns)
        return cell.row * columns + cell.column
    }

    fun cell(index: Int): Cell = Cell(index / columns, index % columns)
}

class Match3Engine(
    private val rows: Int = 8,
    private val columns: Int = 8,
    private val random: Random = Random.Default
) {
    fun createPlayableBoard(): Match3Board {
        repeat(100) {
            var board = emptyBoard()
            for (row in 0 until rows) {
                for (column in 0 until columns) {
                    val cell = Cell(row, column)
                    val forbidden = typesThatWouldMatch(board, cell)
                    val type = GemType.entries.filterNot { it in forbidden }.random(random)
                    board = board.with(cell, Gem(type))
                }
            }
            if (findMatches(board).isEmpty() && hasPossibleMove(board)) return board
        }
        error("Unable to generate playable match-3 board")
    }

    fun swap(board: Match3Board, first: Cell, second: Cell): SwapResult {
        if (!areAdjacent(first, second)) return SwapResult(false, board)
        val firstGem = board[first] ?: return SwapResult(false, board)
        val secondGem = board[second] ?: return SwapResult(false, board)

        var swapped = board.with(first, secondGem).with(second, firstGem)
        if (findMatches(swapped).isEmpty()) return SwapResult(false, board)

        val cascades = mutableListOf<CascadeStep>()
        var combo = 1
        var total = 0
        repeat(30) {
            val matches = findMatches(swapped)
            if (matches.isEmpty()) {
                val playable = if (hasPossibleMove(swapped)) swapped else shuffle(swapped)
                return SwapResult(true, playable, cascades, total)
            }

            val removed = expandSpecialEffects(swapped, matches.flatMap { it.cells }.toSet())
            val stepScore = removed.size * 10 * combo
            total += stepScore
            cascades += CascadeStep(removed, stepScore, combo)
            swapped = collapseAndRefill(remove(swapped, removed))
            combo++
        }
        return SwapResult(true, swapped, cascades, total)
    }

    fun findMatches(board: Match3Board): List<MatchGroup> {
        val groups = mutableListOf<MatchGroup>()

        for (row in 0 until board.rows) {
            var start = 0
            while (start < board.columns) {
                val type = board[Cell(row, start)]?.type
                var end = start + 1
                while (end < board.columns && board[Cell(row, end)]?.type == type && type != null) end++
                if (type != null && end - start >= 3) {
                    groups += MatchGroup((start until end).map { Cell(row, it) }.toSet(), type)
                }
                start = end
            }
        }

        for (column in 0 until board.columns) {
            var start = 0
            while (start < board.rows) {
                val type = board[Cell(start, column)]?.type
                var end = start + 1
                while (end < board.rows && board[Cell(end, column)]?.type == type && type != null) end++
                if (type != null && end - start >= 3) {
                    groups += MatchGroup((start until end).map { Cell(it, column) }.toSet(), type)
                }
                start = end
            }
        }
        return groups
    }

    fun hasPossibleMove(board: Match3Board): Boolean {
        for (row in 0 until board.rows) {
            for (column in 0 until board.columns) {
                val here = Cell(row, column)
                val candidates = listOf(Cell(row + 1, column), Cell(row, column + 1))
                    .filter { it.row in 0 until board.rows && it.column in 0 until board.columns }
                for (other in candidates) {
                    val a = board[here] ?: continue
                    val b = board[other] ?: continue
                    val swapped = board.with(here, b).with(other, a)
                    if (findMatches(swapped).isNotEmpty()) return true
                }
            }
        }
        return false
    }

    fun shuffle(board: Match3Board): Match3Board {
        val gems = board.cells.filterNotNull()
        repeat(100) {
            val shuffled = Match3Board(board.rows, board.columns, gems.shuffled(random))
            if (findMatches(shuffled).isEmpty() && hasPossibleMove(shuffled)) return shuffled
        }
        return createPlayableBoard()
    }

    private fun emptyBoard() = Match3Board(rows, columns, List(rows * columns) { null })

    private fun areAdjacent(a: Cell, b: Cell): Boolean =
        abs(a.row - b.row) + abs(a.column - b.column) == 1

    private fun typesThatWouldMatch(board: Match3Board, cell: Cell): Set<GemType> {
        val forbidden = mutableSetOf<GemType>()
        if (cell.column >= 2) {
            val a = board[Cell(cell.row, cell.column - 1)]?.type
            val b = board[Cell(cell.row, cell.column - 2)]?.type
            if (a != null && a == b) forbidden += a
        }
        if (cell.row >= 2) {
            val a = board[Cell(cell.row - 1, cell.column)]?.type
            val b = board[Cell(cell.row - 2, cell.column)]?.type
            if (a != null && a == b) forbidden += a
        }
        return forbidden
    }

    private fun remove(board: Match3Board, cells: Set<Cell>): Match3Board {
        val next = board.cells.toMutableList()
        cells.forEach { next[board.index(it)] = null }
        return board.copy(cells = next)
    }

    private fun collapseAndRefill(board: Match3Board): Match3Board {
        val next = MutableList<Gem?>(board.cells.size) { null }
        for (column in 0 until board.columns) {
            val existing = (0 until board.rows)
                .mapNotNull { board[Cell(it, column)] }
                .toMutableList()
            while (existing.size < board.rows) existing.add(0, Gem(GemType.entries.random(random)))
            for (row in 0 until board.rows) next[row * board.columns + column] = existing[row]
        }
        return board.copy(cells = next)
    }

    private fun expandSpecialEffects(board: Match3Board, initial: Set<Cell>): Set<Cell> {
        val removed = initial.toMutableSet()
        val queue = ArrayDeque(initial.toList())
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            when (board[cell]?.special ?: SpecialGem.NONE) {
                SpecialGem.NONE -> Unit
                SpecialGem.ROW -> (0 until board.columns).map { Cell(cell.row, it) }
                SpecialGem.COLUMN -> (0 until board.rows).map { Cell(it, cell.column) }
                SpecialGem.BOMB -> buildList {
                    for (r in cell.row - 1..cell.row + 1) for (c in cell.column - 1..cell.column + 1) {
                        if (r in 0 until board.rows && c in 0 until board.columns) add(Cell(r, c))
                    }
                }
                SpecialGem.RAINBOW -> {
                    val target = board.cells.firstOrNull { it != null && it.special == SpecialGem.NONE }?.type
                    if (target == null) emptyList() else board.cells.indices
                        .map(board::cell)
                        .filter { board[it]?.type == target }
                }
            }.forEach { if (removed.add(it)) queue.addLast(it) }
        }
        return removed
    }
}
