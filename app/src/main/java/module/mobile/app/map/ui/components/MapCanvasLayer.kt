package module.mobile.app.map.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import module.mobile.app.R
import module.mobile.app.map.geometry.offsetToCell
import module.mobile.app.map.model.EditorMode
import module.mobile.app.map.model.PoiItem

@Composable
fun MapCanvasLayer(
    imgSizeDp: DpSize,
    mapEditMode: Boolean,
    state: TransformableState,
    scale: Float,
    offset: Offset,
    grid: Array<IntArray>?,
    isCalculating: Boolean,
    editorMode: EditorMode,
    gridVersion: Int,
    matrixRows: Int,
    matrixCols: Int,
    showWalkableCells: Boolean,
    showPoiMarkers: Boolean,
    poiItems: List<PoiItem>,
    selectedPoiId: String?,
    startPoint: Pair<Int, Int>?,
    endPoint: Pair<Int, Int>?,
    path: List<Pair<Int, Int>>?,
    onPoiClick: (PoiItem) -> Unit,
    onTapCell: (Int, Int) -> Unit,
    onDrawStart: (Int, Int) -> Unit,
    onDrawMove: (Pair<Int, Int>, Pair<Int, Int>) -> Unit,
    onDrawEnd: () -> Unit
) {
    BoxWithMapTransform(
        imgSizeDp = imgSizeDp,
        mapEditMode = mapEditMode,
        state = state,
        scale = scale,
        offset = offset
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val markerSize = 20.dp
            val markerBoxSize = 28.dp
            val markerClicksEnabled = editorMode == EditorMode.None

            Image(
                painter = painterResource(id = R.drawable.paint_map),
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier.fillMaxSize()
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(grid, isCalculating, editorMode, gridVersion) {
                        if (grid == null) return@pointerInput
                        detectTapGestures { tapOffset ->
                            val (row, col) = offsetToCell(
                                tapOffset,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                matrixRows,
                                matrixCols
                            )
                            onTapCell(row, col)
                        }
                    }
                    .pointerInput(grid, editorMode) {
                        if (grid == null || (editorMode != EditorMode.DrawOne && editorMode != EditorMode.DrawZero)) {
                            return@pointerInput
                        }

                        var lastCell: Pair<Int, Int>? = null

                        detectDragGestures(
                            onDragStart = { offsetStart ->
                                val (row, col) = offsetToCell(
                                    offsetStart,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    matrixRows,
                                    matrixCols
                                )
                                onDrawStart(row, col)
                                lastCell = Pair(row, col)
                            },
                            onDrag = { change, _ ->
                                val (row, col) = offsetToCell(
                                    change.position,
                                    size.width.toFloat(),
                                    size.height.toFloat(),
                                    matrixRows,
                                    matrixCols
                                )
                                val from = lastCell ?: Pair(row, col)
                                val to = Pair(row, col)
                                onDrawMove(from, to)
                                lastCell = to
                                change.consume()
                            },
                            onDragEnd = {
                                onDrawEnd()
                                lastCell = null
                            },
                            onDragCancel = {
                                lastCell = null
                            }
                        )
                    }
            ) {
                val cellWidth = size.width / matrixCols
                val cellHeight = size.height / matrixRows
                val walkableOverlayColor = if (gridVersion >= 0) Color(0x4D00C853) else Color(0x4D00C853)

                fun getCenter(row: Int, col: Int): Offset {
                    return Offset((col + 0.5f) * cellWidth, (row + 0.5f) * cellHeight)
                }

                if (showWalkableCells) {
                    val currentGrid = grid
                    if (currentGrid != null) {
                        for (r in 0 until matrixRows) {
                            for (c in 0 until matrixCols) {
                                if (currentGrid[r][c] == 1) {
                                    drawRect(
                                        color = walkableOverlayColor,
                                        topLeft = Offset(c * cellWidth, r * cellHeight),
                                        size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight)
                                    )
                                }
                            }
                        }
                    }
                }

                startPoint?.let { (r, c) ->
                    drawCircle(Color.Green, radius = cellWidth * 1.5f, center = getCenter(r, c))
                }
                endPoint?.let { (r, c) ->
                    drawCircle(Color.Red, radius = cellWidth * 1.5f, center = getCenter(r, c))
                }

                path?.let { route ->
                    if (route.isNotEmpty()) {
                        val pathLine = Path().apply {
                            val startCenter = getCenter(route[0].first, route[0].second)
                            moveTo(startCenter.x, startCenter.y)
                            for (i in 1 until route.size) {
                                val c = getCenter(route[i].first, route[i].second)
                                lineTo(c.x, c.y)
                            }
                        }
                        drawPath(pathLine, Color.Blue, style = Stroke(width = cellWidth * 0.8f))
                    }
                }
            }

            if (showPoiMarkers) {
                poiItems.forEach { marker ->
                    val xFraction = (marker.col + 0.5f) / matrixCols.toFloat()
                    val yFraction = (marker.row + 0.5f) / matrixRows.toFloat()
                    val markerColor = poiTint(marker.typeId)
                    val isSelected = marker.id == selectedPoiId

                    Box(
                        modifier = Modifier
                            .offset(
                                x = (maxWidth * xFraction) - markerBoxSize / 2,
                                y = (maxHeight * yFraction) - markerBoxSize / 2
                            )
                            .size(markerBoxSize)
                            .background(
                                color = if (isSelected) Color.White else Color(0xFFF1F9FF),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = markerClicksEnabled) { onPoiClick(marker) },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            imageVector = poiIcon(marker.typeId),
                            contentDescription = marker.name,
                            tint = markerColor,
                            modifier = Modifier.size(markerSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxWithMapTransform(
    imgSizeDp: DpSize,
    mapEditMode: Boolean,
    state: TransformableState,
    scale: Float,
    offset: Offset,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .requiredSize(imgSizeDp)
            .then(if (mapEditMode) Modifier else Modifier.transformable(state = state))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        content()
    }
}

