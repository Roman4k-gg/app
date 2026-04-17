package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import module.mobile.app.R

@Composable
fun ToolsMenuCard(
    showWalkableCells: Boolean,
    onShowWalkableCellsChange: (Boolean) -> Unit,
    showPoiMarkers: Boolean,
    onShowPoiMarkersChange: (Boolean) -> Unit,
    showTapCoordinates: Boolean,
    onShowTapCoordinatesChange: (Boolean) -> Unit,
    ribbonModeEnabled: Boolean,
    onRibbonModeChange: (Boolean) -> Unit,
    onClearRibbons: () -> Unit,
    editorVisible: Boolean,
    onToggleEditor: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(start = 86.dp, top = 8.dp)
            .width(300.dp)
            .zIndex(3f),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, Color(0xFF0072BC)),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Инструменты",
                fontFamily = FontFamily(Font(R.font.manropebold)),
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Показывать клетки 1", fontSize = 14.sp)
                Switch(checked = showWalkableCells, onCheckedChange = onShowWalkableCellsChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Показывать точки интереса", fontSize = 14.sp)
                Switch(checked = showPoiMarkers, onCheckedChange = onShowPoiMarkersChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Показывать координаты тапа", fontSize = 14.sp)
                Switch(checked = showTapCoordinates, onCheckedChange = onShowTapCoordinatesChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Режим лент A*", fontSize = 14.sp)
                Switch(checked = ribbonModeEnabled, onCheckedChange = onRibbonModeChange)
            }

            OutlinedButton(
                onClick = onClearRibbons,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, Color(0xFF0072BC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Очистить ленты", color = Color.Black)
            }

            OutlinedButton(
                onClick = onToggleEditor,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, Color(0xFF0072BC)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (editorVisible) "Закрыть редактор" else "Открыть редактор",
                    color = Color.Black
                )
            }
        }
    }
}

