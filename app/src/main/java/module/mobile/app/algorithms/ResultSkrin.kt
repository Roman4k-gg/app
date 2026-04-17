package module.mobile.app.algorithms

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import module.mobile.app.R

@Composable
fun ResultScreen(
    imagePixels: FloatArray,
    onBackToDraw: () -> Unit,
    onApplyDigit: (Int) -> Unit
) {
    val context = LocalContext.current
    val recognizer = remember { DigitRecognizer(context) }
    var result by remember { mutableStateOf<Int?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                recognizer.loadModel()
                val digit = recognizer.recognize(imagePixels)
                result = digit
            } catch (e: Exception) {
                error = e.message ?: context.getString(R.string.result_recognition_error_default)
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            error != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.result_error_prefix, error ?: ""), color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackToDraw) { Text(stringResource(R.string.common_back)) }
                }
            }
            result != null -> {
                val digit = result ?: 0
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.result_recognized_digit, digit),
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onApplyDigit(digit) }) {
                            Text(stringResource(R.string.result_apply_rating))
                        }
                        OutlinedButton(onClick = onBackToDraw) {
                            Text(stringResource(R.string.result_draw_another))
                        }
                    }
                }
            }
        }
    }
}