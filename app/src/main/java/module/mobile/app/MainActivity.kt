package module.mobile.app

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import module.mobile.app.ui.theme.MobilemoduleTheme

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
    var currentScreen by remember { mutableStateOf("start") }

    when (currentScreen) {
        "start" -> StartScreen(
            onEvaluateClick = { currentScreen = "grid" }
        )
        "grid" -> GridScreen(
            onBackClick = { currentScreen = "start" },
            onNeuralClick = {currentScreen = "neural"}
        )
        "neural" -> NeuralScreen(
            onBackClick = {currentScreen = "start"}
        )
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
fun GridScreen(onBackClick: () -> Unit, onNeuralClick: () -> Unit){
    val itemsList = (0..24).toList()
    val gridState = remember { mutableStateListOf(*Array(25) { 0 }) }

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
                    ) {
                    }
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top=590.dp),
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
fun NeuralScreen(onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("В начало") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "В начало"
                        )
                    }
                }
            )
        }
    ){}
}

