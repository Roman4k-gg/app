package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun TapCoordinatesBadge(row: Int, col: Int, value: Int?, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF0072BC)),
        shadowElevation = 6.dp
    ) {
        Text(
            text = "Координаты: row=$row, col=$col, value=${value ?: "?"}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = Color.Black,
            fontSize = 14.sp
        )
    }
}

