package ru.loveyou.app

import kotlin.random.Random

data class Cell(val row: Int, val column: Int)

enum class Tetromino(val rotations: List<List<Cell>>) {
    I(listOf(
        listOf(Cell(1,0), Cell(1,1), Cell(1,2), Cell(1,3)),
        listOf(Cell(0,2), Cell(1,2), Cell(2,2), Cell(3,2))
    )),
    O(listOf(listOf(Cell(0,0), Cell(0,1), Cell(1,0), Cell(1,1)))),
    T(listOf(
        listOf(Cell(0,1), Cell(1,0), Cell(1,1), Cell(1,2)),
        listOf(Cell(0,1), Cell(1,1), Cell(1,2), Cell(2,1)),
        listOf(Cell(1,0), Cell(1,1), Cell(1,2), Cell(2,1)),
        listOf(Cell(0,1), Cell(1,0), Cell(1,1), Cell(2,1))
    )),
    L(listOf(
        listOf(Cell(0,2), Cell(1,0), Cell(1,1), Cell(1,2)),
        listOf(Cell(0,1), Cell(1,1), Cell(2,1), Cell(2,2)),
        listOf(Cell(1,0), Cell(1,1), Cell(1,2), Cell(2,0)),
        listOf(Cell(0,0), Cell(0,1), Cell(1,1), Cell(2,1))
    )),
    J(listOf(
        listOf(Cell(0,0), Cell(1,0), Cell(1,1), Cell(1,2)),
        listOf(Cell(0,1), Cell(0,2), Cell(1,1), Cell(2,1)),
        listOf(Cell(1,0), Cell(1,1), Cell(1,2), Cell(2,2)),
        listOf(Cell(0,1), Cell(1,1), Cell(2,0), Cell(2,1))
    )),
    S(listOf(
        listOf(Cell(0,1), Cell(0,2), Cell(1,0), Cell(1,1)),
        listOf(Cell(0,1), Cell(1,1), Cell(1,2), Cell(2,2))
    )),
    Z(listOf(
        listOf(Cell(0,0), Cell(0,1), Cell(1,1), Cell(1,2)),
        listOf(Cell(0,2), Cell(1,1), Cell(1,2), Cell(2,1))
    ))
}

data class FallingPiece(
    val type: Tetromino,
    val rotation: Int = 0,
    val row: Int = -1,
    val column: Int = 3
) {
    fun cells(): List<Cell> = type.rotations[rotation % type.rotations.size].map { Cell(it.row + row, it.column + column) }
}

data class TetrisState(
    val rows: Int = 20,
    val columns: Int = 10,
    val board: Set<Cell> = emptySet(),
    val active: FallingPiece,
    val next: Tetromino,
    val score: Int = 0,
    val lines: Int = 0,
    val level: Int = 1,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val gameOver: Boolean = false
) {
    val dropDelayMs: Long get() = (900L - (level - 1) * 65L).coerceAtLeast(160L)
}

data class LockResult(val state: TetrisState, val clearedLines: Int)

object TetrisEngine {
    fun newState(seed: Int = Random.nextInt()): TetrisState {
        val random = Random(seed)
        return TetrisState(active = FallingPiece(Tetromino.entries.random(random)), next = Tetromino.entries.random(random))
    }

    fun move(state: TetrisState, deltaColumn: Int): TetrisState {
        if (state.gameOver) return state
        val candidate = state.active.copy(column = state.active.column + deltaColumn)
        return if (fits(state, candidate)) state.copy(active = candidate) else state
    }

    fun rotate(state: TetrisState): TetrisState {
        if (state.gameOver) return state
        val rotated = state.active.copy(rotation = (state.active.rotation + 1) % state.active.type.rotations.size)
        if (fits(state, rotated)) return state.copy(active = rotated)
        for (kick in listOf(-1, 1, -2, 2)) {
            val kicked = rotated.copy(column = rotated.column + kick)
            if (fits(state, kicked)) return state.copy(active = kicked)
        }
        return state
    }

    fun softDrop(state: TetrisState, random: Random = Random.Default): TetrisState {
        if (state.gameOver) return state
        val down = state.active.copy(row = state.active.row + 1)
        return if (fits(state, down)) state.copy(active = down, score = state.score + 1)
        else lock(state, random).state
    }

    fun hardDrop(state: TetrisState, random: Random = Random.Default): TetrisState {
        if (state.gameOver) return state
        var piece = state.active
        var distance = 0
        while (fits(state, piece.copy(row = piece.row + 1))) {
            piece = piece.copy(row = piece.row + 1)
            distance++
        }
        return lock(state.copy(active = piece, score = state.score + distance * 2), random).state
    }

    fun ghostCells(state: TetrisState): List<Cell> {
        var piece = state.active
        while (fits(state, piece.copy(row = piece.row + 1))) piece = piece.copy(row = piece.row + 1)
        return piece.cells().filter { it.row >= 0 }
    }

    fun fits(state: TetrisState, piece: FallingPiece): Boolean = piece.cells().all {
        it.column in 0 until state.columns && it.row < state.rows && (it.row < 0 || it !in state.board)
    }

    fun lock(state: TetrisState, random: Random = Random.Default): LockResult {
        val placed = state.active.cells().filter { it.row >= 0 }.toSet()
        if (placed.size < 4) return LockResult(state.copy(gameOver = true), 0)
        var board = state.board + placed
        val fullRows = (0 until state.rows).filter { row -> (0 until state.columns).all { Cell(row, it) in board } }
        if (fullRows.isNotEmpty()) {
            val cleared = fullRows.toSet()
            board = board.filterNot { it.row in cleared }.map { cell ->
                val shift = fullRows.count { it > cell.row }
                Cell(cell.row + shift, cell.column)
            }.toSet()
        }
        val combo = if (fullRows.isEmpty()) 0 else state.combo + 1
        val lineScore = when (fullRows.size) { 1 -> 100; 2 -> 300; 3 -> 500; 4 -> 800; else -> 0 }
        val totalLines = state.lines + fullRows.size
        val level = totalLines / 8 + 1
        val nextActive = FallingPiece(state.next)
        val nextState = state.copy(
            board = board,
            active = nextActive,
            next = Tetromino.entries.random(random),
            score = state.score + lineScore * level + combo * 40,
            lines = totalLines,
            level = level,
            combo = combo,
            bestCombo = maxOf(state.bestCombo, combo),
            gameOver = !fits(state.copy(board = board), nextActive)
        )
        return LockResult(nextState, fullRows.size)
    }
}