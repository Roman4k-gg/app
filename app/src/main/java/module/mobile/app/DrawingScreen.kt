package module.mobile.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DrawingScreen(
    onResult: (FloatArray) -> Unit,
    onBack: () -> Unit
) {
    val gridSize = 50
    var pixels by remember { mutableStateOf(Array(gridSize) { BooleanArray(gridSize) { false } }) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    fun setPixel(x: Int, y: Int, value: Boolean) {
        if (x in 0 until gridSize && y in 0 until gridSize) {
            if (pixels[y][x] != value) {
                pixels = pixels.clone().apply { this[y][x] = value }
            }
        }
    }

    fun clear() {
        pixels = Array(gridSize) { BooleanArray(gridSize) { false } }
    }

    fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int) {
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        var x = x0
        var y = y0
        while (true) {
            setPixel(x, y, true)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) { err -= dy; x += sx }
            if (e2 < dx) { err += dx; y += sy }
        }
    }

    var lastPos by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.White)
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (canvasSize.width > 0) {
                                val cellSize = canvasSize.width.toFloat() / gridSize
                                val col = (offset.x / cellSize).roundToInt().coerceIn(0, gridSize - 1)
                                val row = (offset.y / cellSize).roundToInt().coerceIn(0, gridSize - 1)
                                setPixel(col, row, true)
                                lastPos = Offset(col.toFloat(), row.toFloat())
                            }
                        },
                        onDrag = { change, _ ->
                            if (canvasSize.width > 0) {
                                val cellSize = canvasSize.width.toFloat() / gridSize
                                val col = (change.position.x / cellSize).roundToInt().coerceIn(0, gridSize - 1)
                                val row = (change.position.y / cellSize).roundToInt().coerceIn(0, gridSize - 1)
                                lastPos?.let { last ->
                                    val lastCol = last.x.roundToInt()
                                    val lastRow = last.y.roundToInt()
                                    drawLine(lastCol, lastRow, col, row)
                                }
                                setPixel(col, row, true)
                                lastPos = Offset(col.toFloat(), row.toFloat())
                            }
                        },
                        onDragEnd = {
                            lastPos = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (canvasSize.width > 0) {
                    val cellSize = size.width / gridSize
                    // Рисуем чёрные клетки
                    for (row in 0 until gridSize) {
                        for (col in 0 until gridSize) {
                            if (pixels[row][col]) {
                                drawRect(
                                    color = Color.Black,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                )
                            }
                        }
                    }
                    // Рисуем сетку
                    for (i in 0..gridSize) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(i * cellSize, 0f),
                            end = Offset(i * cellSize, size.height),
                            strokeWidth = 0.5f
                        )
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(0f, i * cellSize),
                            end = Offset(size.width, i * cellSize),
                            strokeWidth = 0.5f
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { clear() }) { Text("Очистить") }
            Button(onClick = {
                val flat = FloatArray(gridSize * gridSize) { idx ->
                    val row = idx / gridSize
                    val col = idx % gridSize
                    if (pixels[row][col]) 1f else 0f
                }
                onResult(flat)
            }) { Text("Распознать") }
            Button(onClick = onBack) { Text("Назад") }
        }
    }
}