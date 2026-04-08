package module.mobile.app.map.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import module.mobile.app.algorithms.astar
import module.mobile.app.map.data.loadMatrix
import module.mobile.app.map.data.loadPois
import module.mobile.app.map.data.nextPoiId
import module.mobile.app.map.data.saveEditorExports
import module.mobile.app.map.data.saveMatrix
import module.mobile.app.map.data.savePois
import module.mobile.app.map.geometry.cellsOnLine
import module.mobile.app.map.model.EditorMode
import module.mobile.app.map.model.FoodBonus
import module.mobile.app.map.model.PoiItem
import module.mobile.app.map.model.StudentSpaceBonus
import module.mobile.app.map.ui.components.MapCanvasLayer
import module.mobile.app.map.ui.components.CreatePoiDialog
import module.mobile.app.map.ui.components.EditorPanel
import module.mobile.app.map.ui.components.MapBottomActionBar
import module.mobile.app.map.ui.components.MapBottomActionSheet
import module.mobile.app.map.ui.components.MapTopBar
import module.mobile.app.map.ui.components.MapZoomControls
import module.mobile.app.map.ui.components.PoiListDialog
import module.mobile.app.map.ui.components.TapCoordinatesBadge
import module.mobile.app.map.ui.components.ToolsMenuCard

@Composable
fun MapScreen(goToBackMain: () -> Unit) {
    val imgWidthPx = 4820f
    val imgHeightPx = 2961f
    val matrixRows = 200
    val matrixCols = 325

    val density = LocalDensity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val imgSizeDp = remember(density) {
        DpSize(
            width = (imgWidthPx / density.density).dp,
            height = (imgHeightPx / density.density).dp
        )
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var grid by remember { mutableStateOf<Array<IntArray>?>(null) }
    var gridVersion by remember { mutableStateOf(0) }
    var schemaVersion by remember { mutableStateOf(5) }
    var poiItems by remember { mutableStateOf<List<PoiItem>>(emptyList()) }

    var toolsMenuExpanded by remember { mutableStateOf(false) }
    var editorVisible by remember { mutableStateOf(false) }
    var editorMode by remember { mutableStateOf(EditorMode.None) }
    var showPoiListDialog by remember { mutableStateOf(false) }
    var showCreatePoiDialog by remember { mutableStateOf(false) }
    var pendingPoiCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var newPoiName by remember { mutableStateOf("") }
    var newPoiType by remember { mutableStateOf("landmark") }
    var newPoiMenu by remember { mutableStateOf("coffee") }
    var newPoiOpen by remember { mutableStateOf("09:00") }
    var newPoiClose by remember { mutableStateOf("18:00") }
    var newPoiCapacity by remember { mutableStateOf("20") }
    var newPoiComfort by remember { mutableStateOf("0.5") }

    var showWalkableCells by remember { mutableStateOf(false) }
    var showPoiMarkers by remember { mutableStateOf(true) }
    var showTapCoordinates by remember { mutableStateOf(true) }
    var lastTappedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastTappedValue by remember { mutableStateOf<Int?>(null) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    val bottomMenuScrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedGrid = loadMatrix(context, matrixRows, matrixCols)
            val loadedPoi = loadPois(context, matrixRows, matrixCols)
            withContext(Dispatchers.Main) {
                grid = loadedGrid
                schemaVersion = loadedPoi.first
                poiItems = loadedPoi.second
                gridVersion++
            }
        }
    }

    val mapEditMode = editorVisible && editorMode != EditorMode.None

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        if (mapEditMode || containerSize == IntSize.Zero) return@rememberTransformableState
        val screenWidth = containerSize.width.toFloat()
        val screenHeight = containerSize.height.toFloat()
        val minScale = maxOf(screenWidth / imgWidthPx, screenHeight / imgHeightPx)
        scale = (scale * zoomChange).coerceIn(minScale, 5f)

        val extraWidth = (imgWidthPx * scale - screenWidth).coerceAtLeast(0f) / 2f
        val extraHeight = (imgHeightPx * scale - screenHeight).coerceAtLeast(0f) / 2f
        val nextOffset = offset + panChange
        offset = Offset(
            x = nextOffset.x.coerceIn(-extraWidth, extraWidth),
            y = nextOffset.y.coerceIn(-extraHeight, extraHeight)
        )
    }

    fun applyZoom(zoomFactor: Float) {
        if (containerSize == IntSize.Zero) return
        val screenWidth = containerSize.width.toFloat()
        val screenHeight = containerSize.height.toFloat()
        val minScale = maxOf(screenWidth / imgWidthPx, screenHeight / imgHeightPx)
        scale = (scale * zoomFactor).coerceIn(minScale, 5f)

        val extraWidth = (imgWidthPx * scale - screenWidth).coerceAtLeast(0f) / 2f
        val extraHeight = (imgHeightPx * scale - screenHeight).coerceAtLeast(0f) / 2f
        offset = Offset(
            x = offset.x.coerceIn(-extraWidth, extraWidth),
            y = offset.y.coerceIn(-extraHeight, extraHeight)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MapTopBar(
            onToggleTools = { toolsMenuExpanded = !toolsMenuExpanded },
            onBack = goToBackMain
        )

        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color(0xFF0072BC))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
                .onGloballyPositioned { containerSize = it.size }
                .clipToBounds()
        ) {
            if (toolsMenuExpanded) {
                ToolsMenuCard(
                    showWalkableCells = showWalkableCells,
                    onShowWalkableCellsChange = { showWalkableCells = it },
                    showPoiMarkers = showPoiMarkers,
                    onShowPoiMarkersChange = { showPoiMarkers = it },
                    showTapCoordinates = showTapCoordinates,
                    onShowTapCoordinatesChange = { showTapCoordinates = it },
                    editorVisible = editorVisible,
                    onToggleEditor = {
                        editorVisible = !editorVisible
                        if (!editorVisible) editorMode = EditorMode.None
                    }
                )
            }

            MapCanvasLayer(
                imgSizeDp = imgSizeDp,
                mapEditMode = mapEditMode,
                state = state,
                scale = scale,
                offset = offset,
                grid = grid,
                isCalculating = isCalculating,
                editorMode = editorMode,
                gridVersion = gridVersion,
                matrixRows = matrixRows,
                matrixCols = matrixCols,
                showWalkableCells = showWalkableCells,
                showPoiMarkers = showPoiMarkers,
                poiItems = poiItems,
                startPoint = startPoint,
                endPoint = endPoint,
                path = path,
                onTapCell = { clickedRow, clickedCol ->
                    val currentGrid = grid ?: return@MapCanvasLayer
                    lastTappedCell = Pair(clickedRow, clickedCol)
                    lastTappedValue = currentGrid[clickedRow][clickedCol]

                    when (editorMode) {
                        EditorMode.DrawOne, EditorMode.DrawZero -> {
                            val value = if (editorMode == EditorMode.DrawOne) 1 else 0
                            if (currentGrid[clickedRow][clickedCol] != value) {
                                currentGrid[clickedRow][clickedCol] = value
                                lastTappedValue = value
                                gridVersion++
                                coroutineScope.launch(Dispatchers.IO) {
                                    saveMatrix(context, currentGrid)
                                }
                            }
                        }

                        EditorMode.AddPoi -> {
                            pendingPoiCell = Pair(clickedRow, clickedCol)
                            newPoiName = ""
                            newPoiType = "landmark"
                            newPoiMenu = "coffee"
                            newPoiOpen = "09:00"
                            newPoiClose = "18:00"
                            newPoiCapacity = "20"
                            newPoiComfort = "0.5"
                            showCreatePoiDialog = true
                        }

                        EditorMode.None -> {
                            if (isCalculating) return@MapCanvasLayer
                            if (currentGrid[clickedRow][clickedCol] == 1) {
                                if (startPoint == null || endPoint != null) {
                                    startPoint = Pair(clickedRow, clickedCol)
                                    endPoint = null
                                    path = null
                                } else if (endPoint == null) {
                                    endPoint = Pair(clickedRow, clickedCol)
                                    isCalculating = true
                                    val start = startPoint ?: return@MapCanvasLayer
                                    val goal = endPoint ?: return@MapCanvasLayer

                                    coroutineScope.launch(Dispatchers.Default) {
                                        val calculatedPath = astar(currentGrid, start, goal)
                                        withContext(Dispatchers.Main) {
                                            path = calculatedPath
                                            isCalculating = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                onDrawStart = { row, col ->
                    val currentGrid = grid ?: return@MapCanvasLayer
                    val newValue = if (editorMode == EditorMode.DrawOne) 1 else 0
                    if (currentGrid[row][col] != newValue) {
                        currentGrid[row][col] = newValue
                        gridVersion++
                    }
                    lastTappedCell = Pair(row, col)
                    lastTappedValue = newValue
                },
                onDrawMove = { from, to ->
                    val currentGrid = grid ?: return@MapCanvasLayer
                    val newValue = if (editorMode == EditorMode.DrawOne) 1 else 0
                    val lineCells = cellsOnLine(from, to)
                    var hasChanges = false
                    lineCells.forEach { (r, c) ->
                        if (currentGrid[r][c] != newValue) {
                            currentGrid[r][c] = newValue
                            hasChanges = true
                        }
                    }
                    if (hasChanges) {
                        gridVersion++
                    }
                    lastTappedCell = to
                    lastTappedValue = newValue
                },
                onDrawEnd = {
                    val currentGrid = grid ?: return@MapCanvasLayer
                    coroutineScope.launch(Dispatchers.IO) {
                        saveMatrix(context, currentGrid)
                    }
                }
            )

            if (editorVisible) {
                EditorPanel(
                    modifier = Modifier.align(Alignment.TopEnd),
                    editorMode = editorMode,
                    onSelectMode = { editorMode = it },
                    onShowPoiList = {
                        showPoiListDialog = true
                        editorMode = EditorMode.None
                    },
                    onSave = {
                        val currentGrid = grid ?: return@EditorPanel
                        val currentPois = poiItems
                        coroutineScope.launch(Dispatchers.IO) {
                            saveEditorExports(context, schemaVersion, currentGrid, currentPois)
                        }
                    }
                )
            }

            if (showTapCoordinates) {
                lastTappedCell?.let { (row, col) ->
                    TapCoordinatesBadge(
                        row = row,
                        col = col,
                        value = lastTappedValue,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 12.dp)
                    )
                }
            }

            MapZoomControls(
                onZoomIn = { applyZoom(1.3f) },
                onZoomOut = { applyZoom(0.7f) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp, bottom = 100.dp)
            )

            MapBottomActionBar(
                onToggleSheet = { isBottomSheetVisible = !isBottomSheetVisible },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (isBottomSheetVisible) {
                MapBottomActionSheet(
                    scrollState = bottomMenuScrollState,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }

            if (showCreatePoiDialog && pendingPoiCell != null) {
                val (row, col) = pendingPoiCell ?: Pair(0, 0)
                CreatePoiDialog(
                    row = row,
                    col = col,
                    name = newPoiName,
                    onNameChange = { newPoiName = it },
                    type = newPoiType,
                    onTypeChange = { newPoiType = it },
                    menu = newPoiMenu,
                    onMenuChange = { newPoiMenu = it },
                    open = newPoiOpen,
                    onOpenChange = { newPoiOpen = it },
                    close = newPoiClose,
                    onCloseChange = { newPoiClose = it },
                    capacity = newPoiCapacity,
                    onCapacityChange = { newPoiCapacity = it },
                    comfort = newPoiComfort,
                    onComfortChange = { newPoiComfort = it },
                    onDismiss = { showCreatePoiDialog = false },
                    onCreate = {
                        if (newPoiName.isBlank()) return@CreatePoiDialog
                        val nextId = nextPoiId(poiItems, newPoiType)
                        val newPoi = when (newPoiType) {
                            "food" -> PoiItem(
                                id = nextId,
                                name = newPoiName.trim(),
                                typeId = "food",
                                row = row,
                                col = col,
                                foodBonus = FoodBonus(
                                    menu = newPoiMenu.split(',').map { it.trim() }.filter { it.isNotBlank() },
                                    open = newPoiOpen.trim(),
                                    close = newPoiClose.trim()
                                )
                            )

                            "student_space" -> PoiItem(
                                id = nextId,
                                name = newPoiName.trim(),
                                typeId = "student_space",
                                row = row,
                                col = col,
                                spaceBonus = StudentSpaceBonus(
                                    capacity = newPoiCapacity.toIntOrNull() ?: 20,
                                    comfort = newPoiComfort.toFloatOrNull() ?: 0.5f
                                )
                            )

                            else -> PoiItem(
                                id = nextId,
                                name = newPoiName.trim(),
                                typeId = "landmark",
                                row = row,
                                col = col
                            )
                        }

                        val updated = poiItems + newPoi
                        poiItems = updated
                        showCreatePoiDialog = false
                        showPoiMarkers = true

                        coroutineScope.launch(Dispatchers.IO) {
                            savePois(context, schemaVersion, updated)
                        }
                    }
                )
            }

            if (showPoiListDialog) {
                PoiListDialog(
                    pois = poiItems,
                    onDelete = { poi ->
                        val updated = poiItems.filterNot { it.id == poi.id }
                        poiItems = updated
                        coroutineScope.launch(Dispatchers.IO) {
                            savePois(context, schemaVersion, updated)
                        }
                    },
                    onDismiss = { showPoiListDialog = false }
                )
            }
        }
    }
}



