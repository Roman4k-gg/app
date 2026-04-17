package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import module.mobile.app.R
import module.mobile.app.map.model.EditorMode

@Composable
fun EditorPanel(
    editorMode: EditorMode,
    onSelectMode: (EditorMode) -> Unit,
    onShowPoiList: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .padding(top = 100.dp, end = 8.dp)
            .width(82.dp)
            .zIndex(2f),
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surface,
        border = BorderStroke(2.dp, colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EditorButton(
                text = "1",
                active = editorMode == EditorMode.DrawOne,
                onClick = { onSelectMode(if (editorMode == EditorMode.DrawOne) EditorMode.None else EditorMode.DrawOne) }
            )
            EditorButton(
                text = "0",
                active = editorMode == EditorMode.DrawZero,
                onClick = { onSelectMode(if (editorMode == EditorMode.DrawZero) EditorMode.None else EditorMode.DrawZero) }
            )
            EditorButton(
                text = "+",
                active = editorMode == EditorMode.AddPoi,
                onClick = { onSelectMode(if (editorMode == EditorMode.AddPoi) EditorMode.None else EditorMode.AddPoi) }
            )
            EditorButton(text = stringResource(R.string.editor_button_poi), active = false, onClick = onShowPoiList)
            EditorButton(text = stringResource(R.string.editor_button_save), active = false, onClick = onSave)
        }
    }
}

