package module.mobile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import module.mobile.app.algorithms.DecisionTreeScreen
import module.mobile.app.algorithms.DrawingScreen
import module.mobile.app.algorithms.ResultScreen
import module.mobile.app.map.ui.screen.MapScreen
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
    val navController = rememberNavController()
    var drawingResult by remember { mutableStateOf<FloatArray?>(null) }

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                onMapClick = { navController.navigate("map") },
                onDrawClick = { navController.navigate("draw") },
                onLunchClick = { navController.navigate("lunch") }
            )
        }

        composable("map") {
            MapScreen(goToBackMain = { navController.popBackStack() })
        }

        composable("draw") {
            DrawingScreen(
                onResult = { pixels ->
                    drawingResult = pixels
                    navController.navigate("result")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable("result") {
            ResultScreen(
                imagePixels = drawingResult ?: FloatArray(2500) { 0f },
                onBackToDraw = { navController.popBackStack() }
            )
        }

        composable("lunch") {
            DecisionTreeScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun WelcomeScreen(onMapClick: () -> Unit, onDrawClick: () -> Unit, onLunchClick: () -> Unit) {
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
                color = androidx.compose.ui.graphics.Color.Black
            )

            Button(
                onClick = onMapClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF1F9FF),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF0072BC)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Открыть карту",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }

            Button(
                onClick = onDrawClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF1F9FF),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF0072BC)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Распознать цифру",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }

            Button(
                onClick = onLunchClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFF1F9FF),
                    contentColor = androidx.compose.ui.graphics.Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, androidx.compose.ui.graphics.Color(0xFF0072BC)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = "Где пообедать?",
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }
        }
    }
}
