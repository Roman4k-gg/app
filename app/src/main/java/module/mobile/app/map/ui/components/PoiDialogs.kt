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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Color(0xFF0072BC))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 380.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Новая точка ($row, $col)",
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = type == "landmark", onClick = { onTypeChange("landmark") }, label = { Text("Достопр.") })
                    FilterChip(selected = type == "food", onClick = { onTypeChange("food") }, label = { Text("Еда") })
                    FilterChip(selected = type == "student_space", onClick = { onTypeChange("student_space") }, label = { Text("Пространство") })
                }

                if (type == "food") {
                    OutlinedTextField(
                        value = menu,
                        onValueChange = onMenuChange,
                        label = { Text("menu (через запятую)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = open,
                            onValueChange = onOpenChange,
                            label = { Text("Открытие") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = close,
                            onValueChange = onCloseChange,
                            label = { Text("Закрытие") },
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
                            label = { Text("Вместимость") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = comfort,
                            onValueChange = onComfortChange,
                            label = { Text("Комфорт") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = onCreate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5398F9),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Создать точку")
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
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(2.dp, Color(0xFF0072BC))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 420.dp)
                    .height(460.dp)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Список точек",
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
                            border = BorderStroke(1.dp, Color(0xFF0072BC)),
                            color = Color.White
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
                                    Text("Удалить", color = Color(0xFFC62828))
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
                        Text("Закрыть")
                    }
                }
            }
        }
    }
}

