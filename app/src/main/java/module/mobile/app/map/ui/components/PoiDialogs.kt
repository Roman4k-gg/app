package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import module.mobile.app.map.model.PoiItem

@Composable
fun CreatePoiDialog(
    row: Int,
    col: Int,
    name: String,
    onNameChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    menu: String,
    onMenuChange: (String) -> Unit,
    open: String,
    onOpenChange: (String) -> Unit,
    close: String,
    onCloseChange: (String) -> Unit,
    capacity: String,
    onCapacityChange: (String) -> Unit,
    comfort: String,
    onComfortChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreate: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surface,
            border = BorderStroke(2.dp, colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 380.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.poi_new_point_title, row, col),
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.poi_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == "landmark", onClick = { onTypeChange("landmark") }, label = { Text(stringResource(R.string.poi_type_landmark_short)) })
                    FilterChip(selected = type == "food", onClick = { onTypeChange("food") }, label = { Text(stringResource(R.string.poi_type_food_short)) })
                    FilterChip(selected = type == "student_space", onClick = { onTypeChange("student_space") }, label = { Text(stringResource(R.string.poi_type_space_short)) })
                }

                if (type == "food") {
                    OutlinedTextField(
                        value = menu,
                        onValueChange = onMenuChange,
                        label = { Text(stringResource(R.string.poi_menu_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = open,
                            onValueChange = onOpenChange,
                            label = { Text(stringResource(R.string.poi_open_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = close,
                            onValueChange = onCloseChange,
                            label = { Text(stringResource(R.string.poi_close_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                if (type == "student_space") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = capacity,
                            onValueChange = onCapacityChange,
                            label = { Text(stringResource(R.string.poi_capacity_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = comfort,
                            onValueChange = onComfortChange,
                            label = { Text(stringResource(R.string.poi_comfort_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = onCreate,
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary)
                    ) {
                        Text(stringResource(R.string.poi_create_button))
                    }
                }
            }
        }
    }
}

@Composable
fun PoiListDialog(
    pois: List<PoiItem>,
    onDelete: (PoiItem) -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surface,
            border = BorderStroke(2.dp, colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 420.dp)
                    .height(460.dp)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.poi_list_title),
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pois, key = { it.id }) { poi ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colorScheme.outline),
                            color = colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = poi.name, fontSize = 14.sp)
                                    Text(text = "${poi.typeId} (${poi.row}, ${poi.col})", fontSize = 12.sp, color = Color.Gray)
                                }
                                TextButton(onClick = { onDelete(poi) }) {
                                    Text(stringResource(R.string.common_delete), color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close))
                    }
                }
            }
        }
    }
}

