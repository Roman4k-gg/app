package module.mobile.app.map.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import module.mobile.app.R
import module.mobile.app.algorithms.AntColonyAlgorithm
import module.mobile.app.algorithms.AntCoworkRequest
import module.mobile.app.algorithms.AntTourRequest
import module.mobile.app.algorithms.AstarStep
import module.mobile.app.algorithms.ClusterMetric
import module.mobile.app.algorithms.ClusteringAlgorithm
import module.mobile.app.algorithms.GeneticFoodRequest
import module.mobile.app.algorithms.GeneticFoodRouteAlgorithm
import module.mobile.app.algorithms.astarWithTrace
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
import module.mobile.app.map.ui.components.AntCoworkSettingsDialog
import module.mobile.app.map.ui.components.AntLandmarksDialog
import module.mobile.app.map.ui.components.ClusteringSettingsDialog
import module.mobile.app.map.ui.components.CreatePoiDialog
import module.mobile.app.map.ui.components.EditorPanel
import module.mobile.app.map.ui.components.GeneticCartDialog
import module.mobile.app.map.ui.components.GeneticFoodMenuSheet
import module.mobile.app.map.ui.components.MapBottomActionBar
import module.mobile.app.map.ui.components.MapBottomActionSheet
import module.mobile.app.map.ui.components.MapTopBar
import module.mobile.app.map.ui.components.MapZoomControls
import module.mobile.app.map.ui.components.PoiContextCard
import module.mobile.app.map.ui.components.PoiListDialog
import module.mobile.app.map.ui.components.TapCoordinatesBadge
import module.mobile.app.map.ui.components.ToolsMenuCard
import kotlin.coroutines.resume

@Composable
fun MapScreen(
    goToBackMain: () -> Unit,
    onOpenDecisionTree: () -> Unit,
    onStartRatingDraw: (String) -> Unit,
    pendingRatingUpdate: Pair<String, Int>?,
    onConsumeRatingUpdate: () -> Unit
) {
    val imgWidthPx = 4820f
    val imgHeightPx = 2961f
    val matrixRows = 200
    val matrixCols = 325

    val density = LocalDensity.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var locationPermissionRequestPending by remember { mutableStateOf(false) }

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
    var ribbonModeEnabled by remember { mutableStateOf(false) }
    var ribbonCells by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }
    var showPoiMarkers by remember { mutableStateOf(true) }
    var showTapCoordinates by remember { mutableStateOf(true) }
    var lastTappedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var lastTappedValue by remember { mutableStateOf<Int?>(null) }
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var selectedPoi by remember { mutableStateOf<PoiItem?>(null) }
    var isRouteMode by remember { mutableStateOf(false) }
    var routeSession by remember { mutableStateOf(0) }
    var isCalculating by remember { mutableStateOf(false) }
    var isAstarAnimating by remember { mutableStateOf(false) }
    var astarStepState by remember { mutableStateOf<AstarStep?>(null) }
    var astarAnimationJob by remember { mutableStateOf<Job?>(null) }
    var isBottomSheetVisible by remember { mutableStateOf(false) }
    var isGeneticMenuVisible by remember { mutableStateOf(false) }
    var showGeneticCartDialog by remember { mutableStateOf(false) }
    var geneticCart by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isSelectingGeneticStart by remember { mutableStateOf(false) }
    var isGeneticRunning by remember { mutableStateOf(false) }
    var geneticIteration by remember { mutableStateOf(0) }
    var geneticTotalIterations by remember { mutableStateOf(0) }
    var geneticBestScore by remember { mutableStateOf<Double?>(null) }
    var geneticMissingItemsCount by remember { mutableStateOf(0) }
    var geneticJob by remember { mutableStateOf<Job?>(null) }

    var showAntLandmarksDialog by remember { mutableStateOf(false) }
    var showAntCoworkDialog by remember { mutableStateOf(false) }
    var showClusteringDialog by remember { mutableStateOf(false) }
    var useEuclideanCluster by remember { mutableStateOf(true) }
    var useWalkableCluster by remember { mutableStateOf(false) }
    var clusteringResult by remember { mutableStateOf<module.mobile.app.algorithms.ClusteringResult?>(null) }
    var activeClusterMetric by remember { mutableStateOf(ClusterMetric.Euclidean) }
    var antSelectedLandmarkIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var antStudentsInput by remember { mutableStateOf("") }
    var antStudentCount by remember { mutableStateOf(0) }
    var antPendingAction by remember { mutableStateOf(AntPendingAction.None) }
    var antJob by remember { mutableStateOf<Job?>(null) }
    var isAntRunning by remember { mutableStateOf(false) }
    var antIteration by remember { mutableStateOf(0) }
    var antTotalIterations by remember { mutableStateOf(0) }
    var antBestScore by remember { mutableStateOf<Double?>(null) }
    var antStatusLine by remember { mutableStateOf("") }

    val trySetStartFromLocation: () -> Unit = {
        val currentGrid = grid
        if (currentGrid == null) {
            Toast.makeText(context, context.getString(R.string.map_toast_matrix_not_loaded), Toast.LENGTH_SHORT).show()
        } else {
            coroutineScope.launch {
                val location = getCurrentOrLastKnownLocation(context)
                if (location == null) {
                    Toast.makeText(context, context.getString(R.string.map_toast_location_unavailable), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val rawCell = projectLocationToCell(location.latitude, location.longitude, matrixRows, matrixCols)
                val walkableCell = findNearestWalkableCell(currentGrid, rawCell)
                if (walkableCell == null) {
                    Toast.makeText(context, context.getString(R.string.map_toast_no_walkable_near_location), Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val (row, col) = walkableCell
                lastTappedCell = walkableCell
                lastTappedValue = currentGrid[row][col]

                if (antPendingAction != AntPendingAction.None) {
                    val start = walkableCell
                    val action = antPendingAction
                    antPendingAction = AntPendingAction.None
                    startPoint = start
                    endPoint = null
                    path = null
                    selectedPoi = null

                    val gridSnapshot = Array(currentGrid.size) { r -> currentGrid[r].clone() }
                    antJob?.cancel()
                    antJob = coroutineScope.launch(Dispatchers.Default) {
                        val algorithm = AntColonyAlgorithm()

                        when (action) {
                            AntPendingAction.Landmarks -> {
                                val landmarks = poiItems.filter { it.typeId == "landmark" && it.id in antSelectedLandmarkIds }
                                if (landmarks.isEmpty()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.map_toast_select_at_least_one_landmark),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    return@launch
                                }

                                val request = AntTourRequest(
                                    grid = gridSnapshot,
                                    startCell = start,
                                    landmarks = landmarks
                                )

                                withContext(Dispatchers.Main) {
                                    isAntRunning = true
                                    isCalculating = true
                                    antIteration = 0
                                    antTotalIterations = request.config.iterations
                                    antBestScore = null
                                    antStatusLine = context.getString(
                                        R.string.map_ant_status_tour_points,
                                        landmarks.size
                                    )
                                }

                                try {
                                    algorithm.runLandmarkTour(request).collect { progress ->
                                        withContext(Dispatchers.Main) {
                                            antIteration = progress.iteration
                                            antTotalIterations = progress.totalIterations
                                            antBestScore = progress.bestScore
                                            antStatusLine = context.getString(
                                                R.string.map_ant_status_best_route_points,
                                                progress.bestVisitOrder.size
                                            )
                                            if (progress.bestPath.isNotEmpty()) {
                                                path = progress.bestPath
                                                endPoint = progress.bestPath.last()
                                            }
                                        }
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isAntRunning = false
                                        isCalculating = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.map_toast_ant_tour_finished),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            AntPendingAction.Cowork -> {
                                val spaces = poiItems.filter { it.typeId == "student_space" && it.spaceBonus != null }
                                val request = AntCoworkRequest(
                                    grid = gridSnapshot,
                                    startCell = start,
                                    studentCount = antStudentCount,
                                    spaces = spaces
                                )

                                withContext(Dispatchers.Main) {
                                    isAntRunning = true
                                    isCalculating = true
                                    antIteration = 0
                                    antTotalIterations = request.config.iterations
                                    antBestScore = null
                                    antStatusLine = context.getString(
                                        R.string.map_ant_status_students_in_task,
                                        request.studentCount
                                    )
                                }

                                try {
                                    algorithm.runCoworkOptimization(request).collect { progress ->
                                        withContext(Dispatchers.Main) {
                                            antIteration = progress.iteration
                                            antTotalIterations = progress.totalIterations
                                            antBestScore = progress.bestScore
                                            val primaryName = progress.primarySpace?.name ?: context.getString(R.string.common_none)
                                            val assigned = progress.allocation.sumOf { it.assignedStudents }
                                            antStatusLine = context.getString(
                                                R.string.map_ant_status_primary_cowork,
                                                primaryName,
                                                assigned
                                            )
                                            if (progress.primaryPath.isNotEmpty()) {
                                                path = progress.primaryPath
                                                endPoint = progress.primaryPath.last()
                                            }
                                        }
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) {
                                        isAntRunning = false
                                        isCalculating = false
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.map_toast_cowork_finished),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            AntPendingAction.None -> Unit
                        }
                    }

                    Toast.makeText(
                        context,
                        context.getString(R.string.map_toast_start_set_from_location, walkableCell.first, walkableCell.second),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (isSelectingGeneticStart) {
                    val start = walkableCell
                    isSelectingGeneticStart = false
                    startPoint = start
                    endPoint = null
                    path = null
                    selectedPoi = null

                    val now = Calendar.getInstance()
                    val startMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                    val gridSnapshot = Array(currentGrid.size) { r -> currentGrid[r].clone() }
                    val poisSnapshot = poiItems.toList()
                    val cartSnapshot = geneticCart.toMap()

                    geneticJob?.cancel()
                    geneticJob = coroutineScope.launch(Dispatchers.Default) {
                        val algorithm = GeneticFoodRouteAlgorithm()
                        val request = GeneticFoodRequest(
                            grid = gridSnapshot,
                            startCell = start,
                            cart = cartSnapshot,
                            pois = poisSnapshot,
                            startMinuteOfDay = startMinuteOfDay
                        )

                        withContext(Dispatchers.Main) {
                            isGeneticRunning = true
                            isCalculating = true
                            geneticIteration = 0
                            geneticTotalIterations = request.config.generations
                            geneticBestScore = null
                            geneticMissingItemsCount = cartSnapshot.values.sum()
                        }

                        try {
                            algorithm.run(request).collect { progress ->
                                withContext(Dispatchers.Main) {
                                    geneticIteration = progress.iteration
                                    geneticTotalIterations = progress.totalIterations
                                    geneticBestScore = progress.best.score
                                    geneticMissingItemsCount = progress.best.missingItems.values.sum()
                                    if (progress.best.fullPath.isNotEmpty()) {
                                        path = progress.best.fullPath
                                        endPoint = progress.best.fullPath.last()
                                    }
                                }
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isGeneticRunning = false
                                isCalculating = false
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.map_toast_genetic_finished_missing,
                                        geneticMissingItemsCount
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    Toast.makeText(
                        context,
                        context.getString(R.string.map_toast_start_set_from_location, walkableCell.first, walkableCell.second),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (isRouteMode) {
                    if (startPoint == null || endPoint != null) {
                        startPoint = walkableCell
                        endPoint = null
                        path = null
                    }
                } else {
                    startPoint = walkableCell
                    endPoint = null
                    path = null
                    selectedPoi = null
                }

                Toast.makeText(
                    context,
                    context.getString(R.string.map_toast_start_set_from_location, walkableCell.first, walkableCell.second),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            trySetStartFromLocation()
        } else {
            Toast.makeText(context, context.getString(R.string.map_toast_location_permission_denied), Toast.LENGTH_SHORT).show()
        }
        locationPermissionRequestPending = false
    }

    val bottomMenuScrollState = rememberScrollState()
    val geneticMenuItems by remember(poiItems) {
        derivedStateOf {
            poiItems
                .asSequence()
                .filter { it.typeId == "food" }
                .mapNotNull { it.foodBonus }
                .flatMap { it.menu.asSequence() }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sortedBy { it.lowercase() }
                .toList()
        }
    }
    val landmarkPois by remember(poiItems) {
        derivedStateOf { poiItems.filter { it.typeId == "landmark" } }
    }
    val studentSpacePois by remember(poiItems) {
        derivedStateOf { poiItems.filter { it.typeId == "student_space" && it.spaceBonus != null } }
    }
    val activeClusterAssignments by remember(clusteringResult, activeClusterMetric) {
        derivedStateOf {
            clusteringResult?.assignmentsByMetric?.get(activeClusterMetric).orEmpty()
        }
    }
    val clusterDiffPoiIds by remember(clusteringResult) {
        derivedStateOf { clusteringResult?.differingPoiIds.orEmpty() }
    }
    val activeClusterZones by remember(clusteringResult, activeClusterMetric) {
        derivedStateOf {
            clusteringResult?.zonesByMetric?.get(activeClusterMetric).orEmpty()
        }
    }

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

    LaunchedEffect(locationPermissionRequestPending) {
        if (!locationPermissionRequestPending) return@LaunchedEffect
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(pendingRatingUpdate, poiItems) {
        val update = pendingRatingUpdate ?: return@LaunchedEffect
        if (poiItems.none { it.id == update.first }) return@LaunchedEffect

        val (poiId, rating) = update
        val normalized = rating.coerceIn(0, 9)
        val updated = poiItems.map { item ->
            if (item.id == poiId) item.copy(rating = normalized) else item
        }

        poiItems = updated
        selectedPoi = updated.firstOrNull { it.id == selectedPoi?.id }

        withContext(Dispatchers.IO) {
            savePois(context, schemaVersion, updated)
        }

        Toast.makeText(
            context,
            context.getString(R.string.map_toast_rating_saved, normalized),
            Toast.LENGTH_SHORT
        ).show()
        onConsumeRatingUpdate()
    }

    val mapEditMode = (editorVisible && editorMode != EditorMode.None) || ribbonModeEnabled

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
            onSetStartFromLocation = {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    trySetStartFromLocation()
                } else {
                    locationPermissionRequestPending = true
                }
            },
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
            androidx.compose.animation.AnimatedVisibility(
                visible = toolsMenuExpanded,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(3f),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ToolsMenuCard(
                    showWalkableCells = showWalkableCells,
                    onShowWalkableCellsChange = { showWalkableCells = it },
                    showPoiMarkers = showPoiMarkers,
                    onShowPoiMarkersChange = { showPoiMarkers = it },
                    showTapCoordinates = showTapCoordinates,
                    onShowTapCoordinatesChange = { showTapCoordinates = it },
                    ribbonModeEnabled = ribbonModeEnabled,
                    onRibbonModeChange = { enabled ->
                        ribbonModeEnabled = enabled
                    },
                    onClearRibbons = {
                        ribbonCells = emptySet()
                    },
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
                ribbonModeEnabled = ribbonModeEnabled,
                ribbonCells = ribbonCells,
                showPoiMarkers = showPoiMarkers,
                poiItems = poiItems,
                selectedPoiId = selectedPoi?.id,
                startPoint = startPoint,
                endPoint = endPoint,
                path = path,
                clusterAssignments = activeClusterAssignments,
                clusterZoneCells = activeClusterZones,
                clusterDiffPoiIds = clusterDiffPoiIds,
                astarOpenSet = astarStepState?.openSet ?: emptySet(),
                astarClosedSet = astarStepState?.closedSet ?: emptySet(),
                astarCurrentCell = astarStepState?.current,
                astarAnalyzingCell = astarStepState?.analyzing,
                isAstarAnimating = isAstarAnimating,
                onPoiClick = { poi ->
                    if (isRouteMode || isSelectingGeneticStart || isGeneticRunning || antPendingAction != AntPendingAction.None || isAntRunning) return@MapCanvasLayer
                    selectedPoi = poi
                    lastTappedCell = Pair(poi.row, poi.col)
                    lastTappedValue = grid?.getOrNull(poi.row)?.getOrNull(poi.col)
                },
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

                            if (ribbonModeEnabled && !isRouteMode && !isGeneticRunning && !isAntRunning && antPendingAction == AntPendingAction.None && !isSelectingGeneticStart) {
                                ribbonCells = ribbonCells + Pair(clickedRow, clickedCol)
                                return@MapCanvasLayer
                            }

                            if (antPendingAction != AntPendingAction.None) {
                                if (currentGrid[clickedRow][clickedCol] != 1) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.map_toast_start_must_be_walkable),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@MapCanvasLayer
                                }

                                val start = Pair(clickedRow, clickedCol)
                                val action = antPendingAction
                                antPendingAction = AntPendingAction.None
                                startPoint = start
                                endPoint = null
                                path = null
                                selectedPoi = null

                                val gridSnapshot = Array(currentGrid.size) { row -> currentGrid[row].clone() }

                                antJob?.cancel()
                                antJob = coroutineScope.launch(Dispatchers.Default) {
                                    val algorithm = AntColonyAlgorithm()

                                    when (action) {
                                        AntPendingAction.Landmarks -> {
                                            val selectedLandmarks = landmarkPois.filter { it.id in antSelectedLandmarkIds }
                                            if (selectedLandmarks.isEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.map_toast_select_at_least_one_landmark),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                return@launch
                                            }

                                            val request = AntTourRequest(
                                                grid = gridSnapshot,
                                                startCell = start,
                                                landmarks = selectedLandmarks
                                            )

                                            withContext(Dispatchers.Main) {
                                                isAntRunning = true
                                                isCalculating = true
                                                antIteration = 0
                                                antTotalIterations = request.config.iterations
                                                antBestScore = null
                                                antStatusLine = context.getString(
                                                    R.string.map_ant_status_tour_points,
                                                    selectedLandmarks.size
                                                )
                                            }

                                            try {
                                                algorithm.runLandmarkTour(request).collect { progress ->
                                                    withContext(Dispatchers.Main) {
                                                        antIteration = progress.iteration
                                                        antTotalIterations = progress.totalIterations
                                                        antBestScore = progress.bestScore
                                                        antStatusLine = context.getString(
                                                            R.string.map_ant_status_best_route_points,
                                                            progress.bestVisitOrder.size
                                                        )
                                                        if (progress.bestPath.isNotEmpty()) {
                                                            path = progress.bestPath
                                                            endPoint = progress.bestPath.last()
                                                        }
                                                    }
                                                }
                                            } finally {
                                                withContext(Dispatchers.Main) {
                                                    isAntRunning = false
                                                    isCalculating = false
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.map_toast_ant_tour_finished),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }

                                        AntPendingAction.Cowork -> {
                                            val request = AntCoworkRequest(
                                                grid = gridSnapshot,
                                                startCell = start,
                                                studentCount = antStudentCount,
                                                spaces = studentSpacePois
                                            )

                                            withContext(Dispatchers.Main) {
                                                isAntRunning = true
                                                isCalculating = true
                                                antIteration = 0
                                                antTotalIterations = request.config.iterations
                                                antBestScore = null
                                                antStatusLine = context.getString(
                                                    R.string.map_ant_status_students_in_task,
                                                    request.studentCount
                                                )
                                            }

                                            try {
                                                algorithm.runCoworkOptimization(request).collect { progress ->
                                                    withContext(Dispatchers.Main) {
                                                        antIteration = progress.iteration
                                                        antTotalIterations = progress.totalIterations
                                                        antBestScore = progress.bestScore
                                                        val primaryName = progress.primarySpace?.name
                                                            ?: context.getString(R.string.common_none)
                                                        val assigned = progress.allocation.sumOf { it.assignedStudents }
                                                        antStatusLine = context.getString(
                                                            R.string.map_ant_status_primary_cowork,
                                                            primaryName,
                                                            assigned
                                                        )
                                                        if (progress.primaryPath.isNotEmpty()) {
                                                            path = progress.primaryPath
                                                            endPoint = progress.primaryPath.last()
                                                        }
                                                    }
                                                }
                                            } finally {
                                                withContext(Dispatchers.Main) {
                                                    isAntRunning = false
                                                    isCalculating = false
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.map_toast_cowork_finished),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }

                                        AntPendingAction.None -> Unit
                                    }
                                }
                                return@MapCanvasLayer
                            }

                            if (isSelectingGeneticStart) {
                                if (currentGrid[clickedRow][clickedCol] != 1) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.map_toast_start_must_be_walkable),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@MapCanvasLayer
                                }

                                val start = Pair(clickedRow, clickedCol)
                                isSelectingGeneticStart = false
                                startPoint = start
                                endPoint = null
                                path = null
                                selectedPoi = null

                                val now = Calendar.getInstance()
                                val startMinuteOfDay = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                                val gridSnapshot = Array(currentGrid.size) { row -> currentGrid[row].clone() }
                                val poisSnapshot = poiItems.toList()
                                val cartSnapshot = geneticCart.toMap()

                                geneticJob?.cancel()
                                geneticJob = coroutineScope.launch(Dispatchers.Default) {
                                    val algorithm = GeneticFoodRouteAlgorithm()
                                    val request = GeneticFoodRequest(
                                        grid = gridSnapshot,
                                        startCell = start,
                                        cart = cartSnapshot,
                                        pois = poisSnapshot,
                                        startMinuteOfDay = startMinuteOfDay
                                    )

                                    withContext(Dispatchers.Main) {
                                        isGeneticRunning = true
                                        isCalculating = true
                                        geneticIteration = 0
                                        geneticTotalIterations = request.config.generations
                                        geneticBestScore = null
                                        geneticMissingItemsCount = cartSnapshot.values.sum()
                                    }

                                    try {
                                        algorithm.run(request).collect { progress ->
                                            withContext(Dispatchers.Main) {
                                                geneticIteration = progress.iteration
                                                geneticTotalIterations = progress.totalIterations
                                                geneticBestScore = progress.best.score
                                                geneticMissingItemsCount = progress.best.missingItems.values.sum()
                                                if (progress.best.fullPath.isNotEmpty()) {
                                                    path = progress.best.fullPath
                                                    endPoint = progress.best.fullPath.last()
                                                }
                                            }
                                        }
                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            isGeneticRunning = false
                                            isCalculating = false
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.map_toast_genetic_finished_missing,
                                                    geneticMissingItemsCount
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                                return@MapCanvasLayer
                            }

                            if (isGeneticRunning || isAntRunning) return@MapCanvasLayer

                            if (isRouteMode) {
                                selectedPoi = null
                                if (currentGrid[clickedRow][clickedCol] != 1) return@MapCanvasLayer

                                if (startPoint == null || endPoint != null) {
                                    startPoint = Pair(clickedRow, clickedCol)
                                    endPoint = null
                                    path = null
                                    return@MapCanvasLayer
                                }

                                endPoint = Pair(clickedRow, clickedCol)
                                isCalculating = true
                                val start = startPoint ?: return@MapCanvasLayer
                                val goal = endPoint ?: return@MapCanvasLayer
                                val sessionAtLaunch = routeSession

                                val effectiveGrid = Array(currentGrid.size) { row -> currentGrid[row].clone() }
                                ribbonCells.forEach { (r, c) ->
                                    if (r in 0 until matrixRows && c in 0 until matrixCols) {
                                        effectiveGrid[r][c] = 0
                                    }
                                }

                                coroutineScope.launch(Dispatchers.Default) {
                                    val traced = astarWithTrace(effectiveGrid, start, goal)
                                    withContext(Dispatchers.Main) {
                                        if (isRouteMode && routeSession == sessionAtLaunch) {
                                            astarAnimationJob?.cancel()
                                            astarAnimationJob = launch {
                                                isAstarAnimating = true
                                                for (step in traced.steps) {
                                                    if (!isRouteMode || routeSession != sessionAtLaunch) break
                                                    astarStepState = step
                                                    delay(18)
                                                }
                                                path = traced.path
                                                if (traced.path == null) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.map_toast_route_not_found),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                isAstarAnimating = false
                                                astarStepState = null
                                            }
                                        }
                                        isCalculating = false
                                    }
                                }
                                return@MapCanvasLayer
                            }

                            val clickedPoi = poiItems.firstOrNull { it.row == clickedRow && it.col == clickedCol }
                            if (clickedPoi != null) {
                                selectedPoi = clickedPoi
                                return@MapCanvasLayer
                            }

                            selectedPoi = null
                        }
                    }
                },
                onDrawStart = { row, col ->
                    if (ribbonModeEnabled && editorMode == EditorMode.None) {
                        ribbonCells = ribbonCells + Pair(row, col)
                        lastTappedCell = Pair(row, col)
                        lastTappedValue = 0
                        return@MapCanvasLayer
                    }

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
                    if (ribbonModeEnabled && editorMode == EditorMode.None) {
                        val lineCells = cellsOnLine(from, to)
                        ribbonCells = ribbonCells + lineCells.toSet()
                        lastTappedCell = to
                        lastTappedValue = 0
                        return@MapCanvasLayer
                    }

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
                    if (ribbonModeEnabled && editorMode == EditorMode.None) return@MapCanvasLayer

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
                onToggleSheet = {
                    isBottomSheetVisible = !isBottomSheetVisible
                    if (isBottomSheetVisible) {
                        isGeneticMenuVisible = false
                        showGeneticCartDialog = false
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            if (isRouteMode || isAstarAnimating) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        val statusText = when {
                            startPoint == null -> stringResource(R.string.map_astar_status_select_start)
                            endPoint == null -> stringResource(R.string.map_astar_status_select_end)
                            isAstarAnimating -> stringResource(R.string.map_astar_status_animating)
                            isCalculating -> stringResource(R.string.map_astar_status_calculating)
                            else -> stringResource(R.string.map_astar_status_built)
                        }
                        Text(text = statusText, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = stringResource(
                                R.string.map_ribbons_status,
                                ribbonCells.size,
                                if (ribbonModeEnabled) {
                                    stringResource(R.string.common_enabled)
                                } else {
                                    stringResource(R.string.common_disabled)
                                }
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            if (isSelectingGeneticStart || isGeneticRunning) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = if (isSelectingGeneticStart) {
                                stringResource(R.string.map_genetic_status_select_start)
                            } else {
                                stringResource(
                                    R.string.map_genetic_status_iteration,
                                    geneticIteration,
                                    geneticTotalIterations
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isGeneticRunning) {
                            Text(
                                text = stringResource(
                                    R.string.map_genetic_status_best_score,
                                    geneticBestScore?.let { String.format(Locale.getDefault(), "%.2f", it) }
                                        ?: stringResource(R.string.common_dash),
                                    geneticMissingItemsCount
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            if (antPendingAction != AntPendingAction.None || isAntRunning) {
                val topPadding = if (isSelectingGeneticStart || isGeneticRunning) 86.dp else 12.dp
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(
                            text = if (antPendingAction != AntPendingAction.None) {
                                stringResource(R.string.map_ant_status_select_start)
                            } else {
                                stringResource(
                                    R.string.map_ant_status_iteration,
                                    antIteration,
                                    antTotalIterations
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isAntRunning) {
                            Text(
                                text = stringResource(
                                    R.string.map_ant_status_best_score,
                                    antBestScore?.let { String.format(Locale.getDefault(), "%.2f", it) }
                                        ?: stringResource(R.string.common_dash),
                                    antStatusLine
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            clusteringResult?.let { clusterState ->
                val topPadding = when {
                    antPendingAction != AntPendingAction.None || isAntRunning -> 160.dp
                    isSelectingGeneticStart || isGeneticRunning -> 86.dp
                    isRouteMode || isAstarAnimating -> 86.dp
                    else -> 12.dp
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        val metricLabel = when (activeClusterMetric) {
                            ClusterMetric.Euclidean -> stringResource(R.string.map_cluster_metric_euclidean)
                            ClusterMetric.WalkableAStar -> stringResource(R.string.map_cluster_metric_walkable)
                        }
                        val selectedK = clusterState.selectedKByMetric[activeClusterMetric]
                        Text(
                            text = stringResource(
                                R.string.map_cluster_status,
                                metricLabel,
                                selectedK?.toString() ?: stringResource(R.string.common_dash)
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (clusterState.metrics.size > 1) {
                            Text(
                                text = stringResource(
                                    R.string.map_cluster_diff_points,
                                    clusterState.differingPoiIds.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (ClusterMetric.Euclidean in clusterState.metrics) {
                                    TextButton(onClick = { activeClusterMetric = ClusterMetric.Euclidean }) {
                                        Text(
                                            text = stringResource(R.string.map_cluster_metric_euclidean),
                                            color = if (activeClusterMetric == ClusterMetric.Euclidean) Color(0xFF0072BC) else Color.Gray
                                        )
                                    }
                                }
                                if (ClusterMetric.WalkableAStar in clusterState.metrics) {
                                    TextButton(onClick = { activeClusterMetric = ClusterMetric.WalkableAStar }) {
                                        Text(
                                            text = stringResource(R.string.map_cluster_metric_walkable_short),
                                            color = if (activeClusterMetric == ClusterMetric.WalkableAStar) Color(0xFF0072BC) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = selectedPoi != null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                selectedPoi?.let { poi ->
                    PoiContextCard(
                        poi = poi,
                        onStartRatingDraw = {
                            onStartRatingDraw(poi.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 25.dp, end = 25.dp, bottom = 20.dp)
                    )
                }
            }

            if (isBottomSheetVisible || isGeneticMenuVisible) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isBottomSheetVisible = false
                            isGeneticMenuVisible = false
                            showGeneticCartDialog = false
                        }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isBottomSheetVisible,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .zIndex(2f),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MapBottomActionSheet(
                    scrollState = bottomMenuScrollState,
                    isRouteMode = isRouteMode,
                    onToggleRouteMode = {
                        astarAnimationJob?.cancel()
                        isAstarAnimating = false
                        astarStepState = null
                        geneticJob?.cancel()
                        isGeneticRunning = false
                        isSelectingGeneticStart = false
                        antJob?.cancel()
                        isAntRunning = false
                        antPendingAction = AntPendingAction.None
                        val shouldEnable = !isRouteMode
                        isRouteMode = shouldEnable
                        routeSession += 1
                        isCalculating = false
                        startPoint = null
                        endPoint = null
                        path = null
                        selectedPoi = null
                        isBottomSheetVisible = false
                    },
                    onOpenClusteringMenu = {
                        isBottomSheetVisible = false
                        showClusteringDialog = true
                    },
                    onOpenGeneticMenu = {
                        astarAnimationJob?.cancel()
                        isAstarAnimating = false
                        astarStepState = null
                        antJob?.cancel()
                        isAntRunning = false
                        antPendingAction = AntPendingAction.None
                        isBottomSheetVisible = false
                        isRouteMode = false
                        routeSession += 1
                        isCalculating = false
                        startPoint = null
                        endPoint = null
                        path = null
                        selectedPoi = null
                        isGeneticMenuVisible = true
                    },
                    onOpenAntLandmarks = {
                        astarAnimationJob?.cancel()
                        isAstarAnimating = false
                        astarStepState = null
                        geneticJob?.cancel()
                        isGeneticRunning = false
                        isSelectingGeneticStart = false
                        antJob?.cancel()
                        isAntRunning = false
                        antPendingAction = AntPendingAction.None
                        isBottomSheetVisible = false
                        isGeneticMenuVisible = false
                        showGeneticCartDialog = false
                        showAntLandmarksDialog = true
                        if (antSelectedLandmarkIds.isEmpty()) {
                            antSelectedLandmarkIds = landmarkPois.map { it.id }.toSet()
                        }
                    },
                    onOpenAntCowork = {
                        astarAnimationJob?.cancel()
                        isAstarAnimating = false
                        astarStepState = null
                        geneticJob?.cancel()
                        isGeneticRunning = false
                        isSelectingGeneticStart = false
                        antJob?.cancel()
                        isAntRunning = false
                        antPendingAction = AntPendingAction.None
                        isBottomSheetVisible = false
                        isGeneticMenuVisible = false
                        showGeneticCartDialog = false
                        showAntCoworkDialog = true
                    },
                    onOpenDecisionTree = {
                        isBottomSheetVisible = false
                        onOpenDecisionTree()
                    },
                    modifier = Modifier
                )
            }

            if (isGeneticMenuVisible) {
                GeneticFoodMenuSheet(
                    menuItems = geneticMenuItems,
                    cartCount = geneticCart.values.sum(),
                    onAddToCart = { menuItem ->
                        val current = geneticCart[menuItem] ?: 0
                        geneticCart = geneticCart + (menuItem to (current + 1))
                    },
                    onOpenCart = { showGeneticCartDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .zIndex(3f)
                )
            }

            if (showGeneticCartDialog) {
                GeneticCartDialog(
                    cartItems = geneticCart.entries
                        .map { it.key to it.value }
                        .sortedBy { it.first.lowercase() },
                    onAdd = { menuItem ->
                        val current = geneticCart[menuItem] ?: 0
                        geneticCart = geneticCart + (menuItem to (current + 1))
                    },
                    onRemove = { menuItem ->
                        val current = geneticCart[menuItem]
                        if (current != null) {
                            geneticCart = if (current <= 1) {
                                geneticCart - menuItem
                            } else {
                                geneticCart + (menuItem to (current - 1))
                            }
                        }
                    },
                    onClear = { geneticCart = emptyMap() },
                    onBuy = {
                        astarAnimationJob?.cancel()
                        isAstarAnimating = false
                        astarStepState = null
                        showGeneticCartDialog = false
                        isGeneticMenuVisible = false
                        isBottomSheetVisible = false
                        isRouteMode = false
                        routeSession += 1
                        isCalculating = false
                        selectedPoi = null
                        path = null
                        endPoint = null
                        geneticJob?.cancel()
                        isGeneticRunning = false
                        isSelectingGeneticStart = true
                        antJob?.cancel()
                        isAntRunning = false
                        antPendingAction = AntPendingAction.None
                        Toast.makeText(
                            context,
                            context.getString(R.string.map_toast_select_start_for_genetic),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { showGeneticCartDialog = false }
                )
            }

            if (showAntLandmarksDialog) {
                AntLandmarksDialog(
                    landmarks = landmarkPois,
                    selectedIds = antSelectedLandmarkIds,
                    onToggle = { id ->
                        antSelectedLandmarkIds = if (id in antSelectedLandmarkIds) {
                            antSelectedLandmarkIds - id
                        } else {
                            antSelectedLandmarkIds + id
                        }
                    },
                    onSelectAll = { antSelectedLandmarkIds = landmarkPois.map { it.id }.toSet() },
                    onClear = { antSelectedLandmarkIds = emptySet() },
                    onStart = {
                        if (antSelectedLandmarkIds.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_select_at_least_one_landmark),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AntLandmarksDialog
                        }
                        showAntLandmarksDialog = false
                        antPendingAction = AntPendingAction.Landmarks
                        Toast.makeText(
                            context,
                            context.getString(R.string.map_toast_select_start_on_map),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { showAntLandmarksDialog = false }
                )
            }

            if (showAntCoworkDialog) {
                AntCoworkSettingsDialog(
                    studentsValue = antStudentsInput,
                    onStudentsChange = { antStudentsInput = it },
                    onFindCowork = {
                        val count = antStudentsInput.toIntOrNull()
                        if (count == null || count <= 0) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_invalid_students_count),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AntCoworkSettingsDialog
                        }
                        if (count > 30) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_no_classroom_seats),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AntCoworkSettingsDialog
                        }
                        if (studentSpacePois.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_no_student_spaces),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@AntCoworkSettingsDialog
                        }
                        antStudentCount = count
                        showAntCoworkDialog = false
                        antPendingAction = AntPendingAction.Cowork
                        Toast.makeText(
                            context,
                            context.getString(R.string.map_toast_select_start_on_map),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismiss = { showAntCoworkDialog = false }
                )
            }

            if (showClusteringDialog) {
                ClusteringSettingsDialog(
                    useEuclidean = useEuclideanCluster,
                    onUseEuclideanChange = { useEuclideanCluster = it },
                    useWalkable = useWalkableCluster,
                    onUseWalkableChange = { useWalkableCluster = it },
                    onRun = {
                        val currentGrid = grid
                        if (currentGrid == null) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_matrix_not_loaded),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@ClusteringSettingsDialog
                        }
                        val selectedMetrics = buildSet {
                            if (useEuclideanCluster) add(ClusterMetric.Euclidean)
                            if (useWalkableCluster) add(ClusterMetric.WalkableAStar)
                        }
                        if (selectedMetrics.isEmpty()) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.map_toast_select_at_least_one_metric),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@ClusteringSettingsDialog
                        }
                        val gridSnapshot = Array(currentGrid.size) { row -> currentGrid[row].clone() }
                        val poiSnapshot = poiItems.toList()

                        showClusteringDialog = false
                        isCalculating = true
                        coroutineScope.launch(Dispatchers.Default) {
                            val result = ClusteringAlgorithm().clusterFoodPois(
                                grid = gridSnapshot,
                                pois = poiSnapshot,
                                metrics = selectedMetrics
                            )
                            withContext(Dispatchers.Main) {
                                isCalculating = false
                                clusteringResult = result
                                activeClusterMetric = if (ClusterMetric.Euclidean in result.metrics) {
                                    ClusterMetric.Euclidean
                                } else {
                                    ClusterMetric.WalkableAStar
                                }
                                if (result.assignmentsByMetric.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.map_toast_no_food_pois),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val diffCount = result.differingPoiIds.size
                                    val runInfo = if (result.metrics.size > 1) {
                                        context.getString(R.string.map_toast_clusters_done_with_diff, diffCount)
                                    } else {
                                        context.getString(R.string.map_toast_clusters_done)
                                    }
                                    Toast.makeText(context, runInfo, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onDismiss = { showClusteringDialog = false }
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

private enum class AntPendingAction {
    None,
    Landmarks,
    Cowork
}

private const val MAP_LAT_TOP = 56.4719
private const val MAP_LAT_BOTTOM = 56.4658
private const val MAP_LON_LEFT = 84.9389
private const val MAP_LON_RIGHT = 84.9569

private fun projectLocationToCell(
    latitude: Double,
    longitude: Double,
    rows: Int,
    cols: Int
): Pair<Int, Int> {
    val rowFloat = ((MAP_LAT_TOP - latitude) / (MAP_LAT_TOP - MAP_LAT_BOTTOM)) * (rows - 1)
    val colFloat = ((longitude - MAP_LON_LEFT) / (MAP_LON_RIGHT - MAP_LON_LEFT)) * (cols - 1)

    val row = rowFloat.toInt().coerceIn(0, rows - 1)
    val col = colFloat.toInt().coerceIn(0, cols - 1)
    return row to col
}

private fun findNearestWalkableCell(grid: Array<IntArray>, start: Pair<Int, Int>): Pair<Int, Int>? {
    val rows = grid.size
    if (rows == 0) return null
    val cols = grid.first().size
    val (startRow, startCol) = start
    if (startRow !in 0 until rows || startCol !in 0 until cols) return null
    if (grid[startRow][startCol] == 1) return start

    val visited = Array(rows) { BooleanArray(cols) }
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.add(start)
    visited[startRow][startCol] = true

    val directions = arrayOf(
        -1 to 0,
        1 to 0,
        0 to -1,
        0 to 1
    )

    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeFirst()
        for ((dr, dc) in directions) {
            val nr = row + dr
            val nc = col + dc
            if (nr !in 0 until rows || nc !in 0 until cols) continue
            if (visited[nr][nc]) continue

            if (grid[nr][nc] == 1) return nr to nc

            visited[nr][nc] = true
            queue.add(nr to nc)
        }
    }

    return null
}

private suspend fun getCurrentOrLastKnownLocation(context: Context): Location? = withContext(Dispatchers.IO) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

    val currentProviders = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )

    for (provider in currentProviders) {
        val current = getCurrentLocationOnce(context, locationManager, provider, timeoutMs = 2500L)
        if (current != null) return@withContext current
    }

    // Fallback to cached values only when fresh fix is unavailable.
    getLastKnownLocation(context)
}

private suspend fun getCurrentLocationOnce(
    context: Context,
    locationManager: LocationManager,
    provider: String,
    timeoutMs: Long
): Location? = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        try {
            locationManager.getCurrentLocation(
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context)
            ) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}

private suspend fun getLastKnownLocation(context: Context): Location? = withContext(Dispatchers.IO) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )

    providers
        .asSequence()
        .mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }
}

