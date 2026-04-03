package module.mobile.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import module.mobile.app.ui.theme.MobilemoduleTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.PriorityQueue
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobilemoduleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("veryFirstButt") }
    val gridState = remember { mutableStateListOf(*Array(25) { 0 }) }

    when (currentScreen) {
        "veryFirstButt" -> MainScreen(goToMap = { currentScreen = "firstButt" })
        "firstButt" -> MapScreen(
            goToBackMain = { currentScreen = "veryFirstButt" }
        )
        "start" -> StartScreen(onEvaluateClick = { currentScreen = "grid" })
        "grid" -> GridScreen(
            gridState = gridState,
            onBackClick = { currentScreen = "start" },
            onNeuralClick = { currentScreen = "neural" }
        )
        "neural" -> NeuralScreen(
            gridState = gridState,
            onBackClick = { currentScreen = "start" }
        )
    }
}

@Composable
fun MainScreen(goToMap: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.logo_hits),
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp, vertical = 38.dp)
                .size(60.dp)
                .align(Alignment.TopStart),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Добро пожаловать в HITsGis",
                fontSize = 35.sp,
                lineHeight = 50.sp,
                modifier = Modifier.padding(horizontal = 15.dp),
                fontFamily = FontFamily(Font(R.font.manropebold))
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                thickness = 2.dp,
                color = Color.Black
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .fillMaxHeight(0.23f)
                    .border(2.dp, Color(0xFF0072BC), RoundedCornerShape(10.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.paint_map),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Button(
                onClick = goToMap,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF1F9FF),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Color(0xFF0072BC)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(40.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = "открыть карту",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }
        }
    }
}

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
    var startPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var endPoint by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var path by remember { mutableStateOf<List<Pair<Int, Int>>?>(null) }
    var isCalculating by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("matrix_325_200.txt")
                val lines = inputStream.bufferedReader().readLines()
                val loadedGrid = lines.map { line ->
                    line.trim().split(Regex("\\s+")).map { it.toInt() }.toIntArray()
                }.toTypedArray()
                grid = loadedGrid
            } catch (e: Exception) {
                e.printStackTrace()
                grid = Array(matrixRows) { IntArray(matrixCols) { 1 } }
            }
        }
    }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        if (containerSize == IntSize.Zero) return@rememberTransformableState
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

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo_hits),
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp, top = 25.dp)
                    .size(60.dp)
                    .align(Alignment.CenterStart)
            )
            Button(
                onClick = goToBackMain,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF1F9FF),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 13.dp, top = 45.dp)
                    .size(45.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Color(0xFF0072BC)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.back_home),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                    )
            }
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color(0xFF0072BC))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0E0E0))
                .onGloballyPositioned { containerSize = it.size }
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(imgSizeDp)
                    .transformable(state = state)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.paint_map),
                    contentDescription = null,
                    contentScale = ContentScale.None,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(grid, isCalculating) {
                            if (grid == null || isCalculating) return@pointerInput
                            detectTapGestures { tapOffset ->
                                val percentX = tapOffset.x / size.width
                                val percentY = tapOffset.y / size.height

                                val clickedCol = (percentX * matrixCols).toInt().coerceIn(0, matrixCols - 1)
                                val clickedRow = (percentY * matrixRows).toInt().coerceIn(0, matrixRows - 1)

                                if (grid!![clickedRow][clickedCol] == 1) {
                                    if (startPoint == null || (startPoint != null && endPoint != null)) {
                                        startPoint = Pair(clickedRow, clickedCol)
                                        endPoint = null
                                        path = null
                                    } else if (endPoint == null) {
                                        endPoint = Pair(clickedRow, clickedCol)
                                        isCalculating = true
                                        val currentGrid = grid!!
                                        val start = startPoint!!
                                        val goal = endPoint!!

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
                ) {
                    val cellWidth = size.width / matrixCols
                    val cellHeight = size.height / matrixRows

                    fun getCenter(row: Int, col: Int): Offset {
                        return Offset((col + 0.5f) * cellWidth, (row + 0.5f) * cellHeight)
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

            Column(modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp))
            {
                Button(
                    onClick = {applyZoom(1.3f)},
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F9FF),
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+", fontSize = 22.sp)
                }
                Button(
                    onClick = {applyZoom(0.7f)},
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF1F9FF),
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(2.dp, Color(0xFF0072BC)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("-", fontSize = 24.sp)
                }

            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(420.dp)
                    .padding(bottom = 20.dp, start = 25.dp)
                    .height(100.dp),
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 20.dp,
                border = BorderStroke(width = 2.dp, Color(0xFF0072BC))
            ) {
                Box(modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 25.dp, top = 32.dp)
                )
                {
                    Text(text = "Выберите действие",
                        fontFamily = FontFamily(Font(R.font.manropebold)),
                        fontSize = 22.sp
                    )
                }
            }

        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MapScreenPreview() {
    MapScreen(goToBackMain = {})
}

@Composable
fun StartScreen(onEvaluateClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onEvaluateClick,
            modifier = Modifier
                .width(150.dp)
                .height(60.dp)
        ) {
            Text("Оценка", fontSize = 20.sp, textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    gridState: MutableList<Int>,
    onBackClick: () -> Unit,
    onNeuralClick: () -> Unit
) {
    val itemsList = (0..24).toList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Нарисуйте цифру") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                modifier = Modifier.width(400.dp)
            ) {
                items(itemsList) { index ->
                    val isBlack = gridState[index] == 1
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color.Black)
                            .height(80.dp)
                            .background(if (isBlack) Color.Black else Color.White)
                            .clickable {
                                gridState[index] = if (isBlack) 0 else 1
                            },
                        contentAlignment = Alignment.Center
                    ) { }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 590.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onNeuralClick,
            modifier = Modifier
                .width(150.dp)
                .height(60.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Оставить Оценку", fontSize = 20.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuralScreen(
    gridState: List<Int>,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var predictedDigit by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val classifier = remember { DigitClassifier(context) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.Default) {
                val inputArray = gridStateToModelInput(gridState)
                classifier.predict(inputArray)
            }
            predictedDigit = result + 1
        } catch (e: Exception) {
            errorMessage = e.message ?: "Ошибка при анализе"
        } finally {
            isLoading = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { classifier.close() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Результат") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ошибка: $errorMessage", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) { Text("Вернуться") }
                    }
                }
                predictedDigit != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Распознанная цифра: $predictedDigit",
                            fontSize = 32.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBackClick) { Text("Нарисовать другую") }
                    }
                }
            }
        }
    }
}

private fun gridStateToModelInput(gridState: List<Int>): FloatArray {
    return FloatArray(25) { index -> gridState[index].toFloat() }
}

class DigitClassifier(private val context: android.content.Context) {
    private var interpreter: Interpreter? = null
    init { loadModel() }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("digit_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
    }

    fun predict(input: FloatArray): Int {
        val inputShape = arrayOf(input)
        val outputShape = Array(1) { FloatArray(5) }
        interpreter?.run(inputShape, outputShape) ?: return -1
        val probabilities = outputShape[0]
        var maxIndex = 0
        for (i in probabilities.indices) {
            if (probabilities[i] > probabilities[maxIndex]) {
                maxIndex = i
            }
        }
        return maxIndex
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

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