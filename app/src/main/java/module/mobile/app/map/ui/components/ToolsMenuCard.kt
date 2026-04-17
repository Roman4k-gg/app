package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .padding(start = 86.dp, top = 8.dp)
            .width(300.dp)
            .zIndex(3f),
        color = colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, colorScheme.outline),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.tools_title),
                fontFamily = FontFamily(Font(R.font.manropebold)),
                fontSize = 16.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.tools_show_walkable_cells), fontSize = 14.sp)
                Switch(checked = showWalkableCells, onCheckedChange = onShowWalkableCellsChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.tools_show_poi), fontSize = 14.sp)
                Switch(checked = showPoiMarkers, onCheckedChange = onShowPoiMarkersChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.tools_show_tap_coordinates), fontSize = 14.sp)
                Switch(checked = showTapCoordinates, onCheckedChange = onShowTapCoordinatesChange)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.tools_ribbon_mode_astar), fontSize = 14.sp)
                Switch(checked = ribbonModeEnabled, onCheckedChange = onRibbonModeChange)
            }

            OutlinedButton(
                onClick = onClearRibbons,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = stringResource(R.string.tools_clear_ribbons), color = colorScheme.onSurface)
            }

            OutlinedButton(
                onClick = onToggleEditor,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(2.dp, colorScheme.outline),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = if (editorVisible) {
                        stringResource(R.string.tools_close_editor)
                    } else {
                        stringResource(R.string.tools_open_editor)
                    },
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

