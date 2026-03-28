package module.mobile.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import module.mobile.app.ui.theme.MobilemoduleTheme
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize

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
    var currentScreen by remember { mutableStateOf("firstButt") }
    val gridState = remember { mutableStateListOf(*Array(25) { 0 }) }

    when (currentScreen) {
        "firstButt" -> MapScreen(
            onGradeClick = { currentScreen = "start"}
        )
        "start" -> StartScreen(
            onEvaluateClick = { currentScreen = "grid" }
        )
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
fun MapScreen(onGradeClick: () -> Unit) {
    val imgWidthPx = 4820f
    val imgHeightPx = 2961f

    val density = LocalDensity.current
    
    val imgSizeDp = remember(density) {
        DpSize(
            width = (imgWidthPx / density.density).dp,
            height = (imgHeightPx / density.density).dp
        )
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { containerSize = it.size }
            .clipToBounds()
            .transformable(state = state)
    ) {
        Image(
            painter = painterResource(id = R.drawable.paint_map),
            contentDescription = null,
            contentScale = ContentScale.None,
            modifier = Modifier
                .requiredSize(imgSizeDp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )

        Button(
            onClick = onGradeClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text("Оценка интерфейса")
        }
    }
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

    val classifier = remember {
        DigitClassifier(context)
    }

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
        onDispose {
            classifier.close()
        }
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
                isLoading -> {
                    CircularProgressIndicator()
                }
                errorMessage != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ошибка: $errorMessage", color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBackClick) {
                            Text("Вернуться")
                        }
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
                        Button(onClick = onBackClick) {
                            Text("Нарисовать другую")
                        }
                    }
                }
            }
        }
    }
}

private fun gridStateToModelInput(gridState: List<Int>): FloatArray {
    return FloatArray(25) { index ->
        gridState[index].toFloat()
    }
}

class DigitClassifier(private val context: android.content.Context) {
    private var interpreter: Interpreter? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Не удалось загрузить модель: ${e.message}", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("digit_model.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(input: FloatArray): Int {
        val inputShape = arrayOf(input)
        val outputShape = Array(1) { FloatArray(5) }

        interpreter?.run(inputShape, outputShape)
            ?: throw IllegalStateException("Интерпретатор не инициализирован")

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