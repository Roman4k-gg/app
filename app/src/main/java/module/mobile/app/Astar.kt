package module.mobile.app

import kotlin.math.abs
import java.util.PriorityQueue

data class Node(
    val x: Int,
    val y: Int,
    val g: Int,
    val f: Int
)

fun heuristic(a: Pair<Int, Int>, b: Pair<Int, Int>): Int {
    return abs(a.first - b.first) + abs(a.second - b.second)
}

fun astar(grid: Array<IntArray>, start: Pair<Int, Int>, goal: Pair<Int, Int>): List<Pair<Int, Int>>? {
    val rows = grid.size
    val cols = grid[0].size

    val openSet = PriorityQueue<Node>(compareBy { it.f })
    openSet.add(Node(start.first, start.second, 0, heuristic(start, goal)))

    val cameFrom = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()
    val gScore = mutableMapOf<Pair<Int, Int>, Int>()
    gScore[start] = 0

    val directions = listOf(
        Pair(1, 0), Pair(-1, 0),
        Pair(0, 1), Pair(0, -1)
    )

    while (openSet.isNotEmpty()) {
        val current = openSet.poll()
        val currentPos = Pair(current.x, current.y)

        if (currentPos == goal) {
            val path = mutableListOf<Pair<Int, Int>>()
            var cur = goal
            while (cur in cameFrom) {
                path.add(cur)
                cur = cameFrom[cur]!!
            }
            path.add(start)
            return path.reversed()
        }

        for ((dx, dy) in directions) {
            val nx = current.x + dx
            val ny = current.y + dy
            val neighbor = Pair(nx, ny)

            if (nx !in 0 until rows || ny !in 0 until cols) continue
            if (grid[nx][ny] == 0) continue

            val tentativeG = gScore[currentPos]!! + 1

            if (neighbor !in gScore || tentativeG < gScore[neighbor]!!) {
                cameFrom[neighbor] = currentPos
                gScore[neighbor] = tentativeG
                val f = tentativeG + heuristic(neighbor, goal)
                openSet.add(Node(nx, ny, tentativeG, f))
            }
        }
    }
    return null
}