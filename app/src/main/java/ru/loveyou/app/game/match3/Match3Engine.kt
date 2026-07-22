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
    fun createPlayableBoard(): Match3Board = createPlayableBoard(rows, columns)

    fun swap(board: Match3Board, first: Cell, second: Cell): SwapResult {
        if (!areAdjacent(first, second)) return SwapResult(false, board)
        val firstGem = board[first] ?: return SwapResult(false, board)
        val secondGem = board[second] ?: return SwapResult(false, board)

        var swapped = board.with(first, secondGem).with(second, firstGem)
        if (findMatches(swapped).isEmpty()) return SwapResult(false, board)

        val cascades = mutableListOf<CascadeStep>()
        var combo = 1
        var total = 0
        repeat(MAX_CASCADES) {
            val matches = findMatches(swapped)
            if (matches.isEmpty()) {
                val playable = if (hasPossibleMove(swapped)) swapped else shuffle(swapped)
                return SwapResult(true, playable, cascades, total)
            }

            val removed = expandSpecialEffects(swapped, matches.flatMap { it.cells }.toSet())
            val stepScore = removed.size * BASE_GEM_SCORE * combo
            total += stepScore
            cascades += CascadeStep(removed, stepScore, combo)
            swapped = collapseAndRefill(remove(swapped, removed))
            combo++
        }

        val stable = settleBoard(swapped)
        return SwapResult(true, stable, cascades, total)
    }

    fun findMatches(board: Match3Board): List<MatchGroup> {
        val groups = mutableListOf<MatchGroup>()

        for (row in 0 until board.rows) {
            var start = 0
            while (start < board.columns) {
                val type = board[Cell(row, start)]?.type
                var end = start + 1
                while (end < board.columns && type != null && board[Cell(row, end)]?.type == type) end++
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
                while (end < board.rows && type != null && board[Cell(end, column)]?.type == type) end++
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
        if (gems.size == board.cells.size) {
            repeat(GENERATION_ATTEMPTS) {
                val shuffled = Match3Board(board.rows, board.columns, gems.shuffled(random))
                if (findMatches(shuffled).isEmpty() && hasPossibleMove(shuffled)) return shuffled
            }
        }
        return createPlayableBoard(board.rows, board.columns)
    }

    private fun createPlayableBoard(boardRows: Int, boardColumns: Int): Match3Board {
        repeat(GENERATION_ATTEMPTS) {
            var board = Match3Board(boardRows, boardColumns, List(boardRows * boardColumns) { null })
            for (row in 0 until boardRows) {
                for (column in 0 until boardColumns) {
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

    private fun settleBoard(start: Match3Board): Match3Board {
        var board = start
        repeat(MAX_CASCADES) {
            val matches = findMatches(board)
            if (matches.isEmpty()) return if (hasPossibleMove(board)) board else shuffle(board)
            val removed = matches.flatMap { it.cells }.toSet()
            board = collapseAndRefill(remove(board, removed))
        }
        return shuffle(board)
    }

    private fun expandSpecialEffects(board: Match3Board, initial: Set<Cell>): Set<Cell> {
        val removed = initial.toMutableSet()
        val queue = ArrayDeque(initial.toList())
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            val affected: List<Cell> = when (board[cell]?.special ?: SpecialGem.NONE) {
                SpecialGem.NONE -> emptyList()
                SpecialGem.ROW -> (0 until board.columns).map { Cell(cell.row, it) }
                SpecialGem.COLUMN -> (0 until board.rows).map { Cell(it, cell.column) }
                SpecialGem.BOMB -> buildList {
                    for (row in cell.row - 1..cell.row + 1) {
                        for (column in cell.column - 1..cell.column + 1) {
                            if (row in 0 until board.rows && column in 0 until board.columns) {
                                add(Cell(row, column))
                            }
                        }
                    }
                }
                SpecialGem.RAINBOW -> {
                    val target = board.cells.firstOrNull {
                        it != null && it.special == SpecialGem.NONE
                    }?.type
                    if (target == null) emptyList() else board.cells.indices
                        .map(board::cell)
                        .filter { board[it]?.type == target }
                }
            }
            affected.forEach { affectedCell ->
                if (removed.add(affectedCell)) queue.addLast(affectedCell)
            }
        }
        return removed
    }

    private companion object {
        const val BASE_GEM_SCORE = 10
        const val GENERATION_ATTEMPTS = 100
        const val MAX_CASCADES = 30
    }
}
