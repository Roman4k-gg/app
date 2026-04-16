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
import androidx.compose.material3.Checkbox
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
fun AntLandmarksDialog(
    landmarks: List<PoiItem>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit,
    onStart: () -> Unit,
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
                    .widthIn(min = 340.dp, max = 430.dp)
                    .height(500.dp)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Выбор достопримечательностей",
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp
                )
                Text(
                    text = "Отмечено: ${selectedIds.size}",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(landmarks, key = { it.id }) { poi ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF0072BC)),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = poi.id in selectedIds,
                                    onCheckedChange = { onToggle(poi.id) }
                                )
                                Text(
                                    text = poi.name,
                                    modifier = Modifier.padding(start = 6.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        TextButton(onClick = onSelectAll) { Text("Все") }
                        TextButton(onClick = onClear) { Text("Очистить") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) { Text("Отмена") }
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5398F9),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(2.dp, Color(0xFF0072BC))
                        ) {
                            Text("Выбрать старт")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AntCoworkSettingsDialog(
    studentsValue: String,
    onStudentsChange: (String) -> Unit,
    onFindCowork: () -> Unit,
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
                    .widthIn(min = 320.dp, max = 380.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Подбор коворкинга",
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 18.sp
                )
                Text(
                    text = "Введите размер группы студентов",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = studentsValue,
                    onValueChange = { text -> onStudentsChange(text.filter { it.isDigit() }) },
                    label = { Text("Количество студентов") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Button(
                        onClick = onFindCowork,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF5398F9),
                            contentColor = Color.White
                        ),
                        border = BorderStroke(2.dp, Color(0xFF0072BC))
                    ) {
                        Text("Найти коворк")
                    }
                }
            }
        }
    }
}

