package module.mobile.app.map.geometry

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

fun offsetToCell(
    offset: Offset,
    width: Float,
    height: Float,
    matrixRows: Int,
    matrixCols: Int
): Pair<Int, Int> {
    val percentX = if (width == 0f) 0f else offset.x / width
    val percentY = if (height == 0f) 0f else offset.y / height
    val col = (percentX * matrixCols).toInt().coerceIn(0, matrixCols - 1)
    val row = (percentY * matrixRows).toInt().coerceIn(0, matrixRows - 1)
    return Pair(row, col)
}

fun cellsOnLine(from: Pair<Int, Int>, to: Pair<Int, Int>): List<Pair<Int, Int>> {
    val points = mutableListOf<Pair<Int, Int>>()
    var x0 = from.second
    var y0 = from.first
    val x1 = to.second
    val y1 = to.first

    val dx = abs(x1 - x0)
    val dy = abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1
    val sy = if (y0 < y1) 1 else -1
    var err = dx - dy

    while (true) {
        points.add(Pair(y0, x0))
        if (x0 == x1 && y0 == y1) break
        val e2 = 2 * err
        if (e2 > -dy) {
            err -= dy
            x0 += sx
        }
        if (e2 < dx) {
            err += dx
            y0 += sy
        }
    }
    return points
}


