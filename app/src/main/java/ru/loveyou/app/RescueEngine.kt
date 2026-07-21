package ru.loveyou.app

import kotlin.random.Random

data class MazeCell(val row: Int, val col: Int)

data class RescueMaze(
    val rows: Int,
    val cols: Int,
    val start: MazeCell,
    val goal: MazeCell,
    val open: Set<MazeCell>,
    val shortestPath: List<MazeCell>
) {
    fun contains(cell: MazeCell) = cell.row in 0 until rows && cell.col in 0 until cols
    fun canMove(from: MazeCell, to: MazeCell): Boolean =
        contains(to) && to in open && kotlin.math.abs(from.row - to.row) + kotlin.math.abs(from.col - to.col) == 1
}

object RescueEngine {
    fun generate(level: Int, seed: Int = level * 7919): RescueMaze {
        val size = (7 + (level - 1) / 2 * 2).coerceAtMost(15)
        val random = Random(seed)
        val start = MazeCell(size - 1, 0)
        val goal = MazeCell(0, size - 1)
        val path = mutableListOf(start)
        var current = start
        val visited = mutableSetOf(start)

        while (current != goal) {
            val options = buildList {
                if (current.row > goal.row) add(MazeCell(current.row - 1, current.col))
                if (current.col < goal.col) add(MazeCell(current.row, current.col + 1))
            }.filterNot { it in visited }
            val next = options.random(random)
            path += next
            visited += next
            current = next
        }

        val open = path.toMutableSet()
        val branchBudget = (size + level * 2).coerceAtMost(size * size / 2)
        repeat(branchBudget) {
            val base = open.random(random)
            val neighbors = neighbors(base, size, size).filterNot { it in open }
            if (neighbors.isNotEmpty()) {
                val branch = neighbors.random(random)
                open += branch
                if (random.nextBoolean()) {
                    val extension = neighbors(branch, size, size).filterNot { it in open }
                    if (extension.isNotEmpty()) open += extension.random(random)
                }
            }
        }
        return RescueMaze(size, size, start, goal, open, path)
    }

    fun reachable(maze: RescueMaze): Boolean {
        val queue = ArrayDeque<MazeCell>()
        val seen = mutableSetOf(maze.start)
        queue += maze.start
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            if (cell == maze.goal) return true
            neighbors(cell, maze.rows, maze.cols).filter { it in maze.open && it !in seen }.forEach {
                seen += it
                queue += it
            }
        }
        return false
    }

    private fun neighbors(cell: MazeCell, rows: Int, cols: Int) = listOf(
        MazeCell(cell.row - 1, cell.col), MazeCell(cell.row + 1, cell.col),
        MazeCell(cell.row, cell.col - 1), MazeCell(cell.row, cell.col + 1)
    ).filter { it.row in 0 until rows && it.col in 0 until cols }
}
