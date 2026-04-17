package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import module.mobile.app.R

@Composable
fun ClusteringSettingsDialog(
    useEuclidean: Boolean,
    onUseEuclideanChange: (Boolean) -> Unit,
    useWalkable: Boolean,
    onUseWalkableChange: (Boolean) -> Unit,
    onRun: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Color(0xFF0072BC))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 390.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.clustering_title),
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp
                )
                Text(
                    text = stringResource(R.string.clustering_subtitle),
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.clustering_metric_euclidean), fontSize = 14.sp)
                    Checkbox(checked = useEuclidean, onCheckedChange = onUseEuclideanChange)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.clustering_metric_walkable), fontSize = 14.sp)
                    Checkbox(checked = useWalkable, onCheckedChange = onUseWalkableChange)
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = onRun,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5398F9),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, Color(0xFF0072BC))
                    ) {
                        Text(stringResource(R.string.clustering_build_button))
                    }
                }
            }
        }
    }
}

