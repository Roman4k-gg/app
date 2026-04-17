package module.mobile.app

import android.app.LocaleManager
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }

            MobilemoduleTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}


@Composable
fun AppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val localeManager = remember(context) { context.getSystemService(LocaleManager::class.java) }
    var drawingResult by remember { mutableStateOf<FloatArray?>(null) }
    var ratingPoiId by remember { mutableStateOf<String?>(null) }
    var pendingRatingUpdate by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var currentLanguage by remember {
        mutableStateOf(
            localeManager?.applicationLocales?.let { locales ->
                if (!locales.isEmpty) locales[0].language else null
            } ?: Locale.getDefault().language
        )
    }

    NavHost(
        navController = navController,
        startDestination = "welcome"
    ) {
        composable("welcome") {
            WelcomeScreen(
                onMapClick = { navController.navigate("map") },
                currentLanguage = currentLanguage,
                isDarkTheme = isDarkTheme,
                onToggleLanguage = {
                    val nextLanguage = if (currentLanguage.startsWith("ru")) "en" else "ru"
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(nextLanguage)
                    currentLanguage = nextLanguage
                },
                onToggleTheme = onToggleTheme
            )
        }

        composable("map") {
            MapScreen(
                goToBackMain = { navController.popBackStack() },
                onOpenDecisionTree = { navController.navigate("lunch") },
                onStartRatingDraw = { poiId ->
                    ratingPoiId = poiId
                    navController.navigate("draw")
                },
                pendingRatingUpdate = pendingRatingUpdate,
                onConsumeRatingUpdate = { pendingRatingUpdate = null }
            )
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
                onBackToDraw = { navController.popBackStack() },
                onApplyDigit = { digit ->
                    ratingPoiId?.let { poiId ->
                        pendingRatingUpdate = poiId to digit.coerceIn(0, 9)
                    }
                    ratingPoiId = null
                    drawingResult = null
                    navController.popBackStack("map", false)
                }
            )
        }

        composable("lunch") {
            DecisionTreeScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun WelcomeScreen(
    onMapClick: () -> Unit,
    currentLanguage: String,
    isDarkTheme: Boolean,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
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
                text = stringResource(R.string.welcome_title),
                fontSize = 35.sp,
                lineHeight = 50.sp,
                modifier = Modifier.padding(horizontal = 15.dp),
                fontFamily = FontFamily(Font(R.font.manropebold)),
                color = colorScheme.onBackground
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                thickness = 2.dp,
                color = colorScheme.outline
            )

            Button(
                onClick = onMapClick,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, colorScheme.outline),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_open_map),
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }

            OutlinedButton(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, colorScheme.outline),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.welcome_change_language,
                        if (currentLanguage.startsWith("ru")) {
                            stringResource(R.string.language_russian)
                        } else {
                            stringResource(R.string.language_english)
                        }
                    ),
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }

            OutlinedButton(
                onClick = onToggleTheme,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, colorScheme.outline),
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(56.dp)
            ) {
                Text(
                    text = if (isDarkTheme) {
                        stringResource(R.string.welcome_switch_to_light_theme)
                    } else {
                        stringResource(R.string.welcome_switch_to_dark_theme)
                    },
                    fontSize = 16.sp,
                    fontFamily = FontFamily(Font(R.font.manropebold))
                )
            }
        }
    }
}
