package module.mobile.app

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var isBottomSheetVisible by remember { mutableStateOf(false) }

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
        // Верхняя панель
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
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp, color = Color(0xFF0072BC))

        // Область карты
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

                androidx.compose.foundation.Canvas(
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

            // Кнопки зума
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { applyZoom(1.3f) },
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
                    onClick = { applyZoom(0.7f) },
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

            // Нижняя панель (кнопка вызова меню)
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
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Выберите действие",
                        fontFamily = FontFamily(Font(R.font.manropebold)),
                        fontSize = 22.sp
                    )
                    Button(
                        onClick = { isBottomSheetVisible = !isBottomSheetVisible },
                        modifier = Modifier.size(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5398F9)
                        ),
                        border = BorderStroke(2.dp, Color(0xFF0072BC))
                    ) {
                        // Можно добавить иконку, но оставлено пустым как в оригинале
                    }
                }
            }

            // Выдвижная панель (BottomSheet)
            if (isBottomSheetVisible) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 20.dp,
                    border = BorderStroke(width = 2.dp, Color(0xFF0072BC))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Выберите действие",
                            fontFamily = FontFamily(Font(R.font.manropebold)),
                            fontSize = 22.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedButton(
                            onClick = { /* можно реализовать построение маршрута отдельно */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color(0xFF0072BC))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Построить маршрут", color = Color.Black, fontSize = 16.sp)
                            }
                        }
                        OutlinedButton(
                            onClick = { /* заведения */ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(2.dp, Color(0xFF0072BC))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Заведения", color = Color.Black, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}