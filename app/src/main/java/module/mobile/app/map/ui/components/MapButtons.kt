package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ActionButton(
    text: String,
    active: Boolean = false,
    onClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) colorScheme.secondaryContainer else colorScheme.surface,
            contentColor = colorScheme.onSurface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = colorScheme.onSurface, fontSize = 16.sp)
        }
    }
}

@Composable
fun EditorButton(text: String, active: Boolean, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) colorScheme.secondaryContainer else colorScheme.surface,
            contentColor = colorScheme.onSurface
        ),
        border = BorderStroke(2.dp, colorScheme.outline)
    ) {
        Text(text = text, fontSize = 18.sp)
    }
}


