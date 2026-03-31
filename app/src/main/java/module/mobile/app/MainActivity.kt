package module.mobile.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import org.w3c.dom.Text

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
        "veryFirstButt" -> MainScreen (
            goToMap = { currentScreen = "firstButt"}
        )
        "firstButt" -> MapScreen(
            onGradeClick = { currentScreen = "start"},
            goToBackMain = { currentScreen = "veryFirstButt"}
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
fun MainScreen(goToMap: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.logo_hits),
            contentDescription = null,
            modifier = Modifier
                .padding(16.dp, vertical = 32.dp) // Отступы от краев экрана
                .size(60.dp)
                .align(Alignment.TopStart), // Вот это прижмет его в угол
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

            Divider(
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

//@Preview(
//    showBackground = true,
//    showSystemUi = true
//)
//@Composable
//fun MainScreenPreview() {
//    MainScreen(
//        goToMap = {} // пустая лямбда для превью
//    )
//}
@Composable
fun MapScreen(onGradeClick: () -> Unit, goToBackMain: () -> Unit) {
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
            ) {
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 2.dp,
            color = Color(0xFF0072BC)
        )
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
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MapScreenPreview() {
    MapScreen(
        onGradeClick = {},
        goToBackMain = {}// пустая лямбда для превью
    )
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