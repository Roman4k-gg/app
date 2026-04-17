package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import module.mobile.app.R

@Composable
fun TapCoordinatesBadge(row: Int, col: Int, value: Int?, modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        color = colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, colorScheme.outline),
        shadowElevation = 6.dp
    ) {
        Text(
            text = stringResource(R.string.tap_coordinates_badge, row, col, value?.toString() ?: stringResource(R.string.common_unknown_value)),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}

