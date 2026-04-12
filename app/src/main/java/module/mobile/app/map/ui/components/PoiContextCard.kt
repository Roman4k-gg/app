package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import module.mobile.app.R
import module.mobile.app.map.model.PoiItem

@Composable
fun PoiContextCard(
    poi: PoiItem,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Surface(
        modifier = modifier,
        color = Color(0xFFF9F9F9),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(3.dp, Color(0xFF0072BC)),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFFEAF4FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = poiIcon(poi.typeId),
                        contentDescription = null,
                        tint = poiTint(poi.typeId),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = poi.name,
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 28.sp,
                    lineHeight = 30.sp,
                    color = Color.Black
                )
            }

            HorizontalDivider(thickness = 2.dp, color = Color(0xFF0072BC))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                poi.foodBonus?.let { food ->
                    InfoRow(label = "Время работы", value = "${food.open} - ${food.close}")
                    if (food.menu.isNotEmpty()) {
                        SectionTitle("Меню")
                        food.menu.forEach { item ->
                            Text(text = "- $item", fontSize = 15.sp, color = Color.Black)
                        }
                    }
                }

                poi.spaceBonus?.let { space ->
                    InfoRow(label = "Вместимость", value = space.capacity.toString())
                    InfoRow(label = "Удобство", value = space.comfort.toString())
                }

                if (poi.foodBonus == null && poi.spaceBonus == null) {
                    Text(
                        text = "Дополнительные данные для этой точки пока не заполнены.",
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = FontFamily(Font(R.font.manropebold)),
        fontSize = 18.sp,
        color = Color(0xFF0B4B8F)
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        fontSize = 16.sp,
        color = Color.Black
    )
}

