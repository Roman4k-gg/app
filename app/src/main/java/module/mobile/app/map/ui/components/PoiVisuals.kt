package module.mobile.app.map.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun poiIcon(typeId: String): ImageVector {
    return when (typeId) {
        "food" -> Icons.Filled.Restaurant
        "student_space" -> Icons.Filled.School
        else -> Icons.Filled.AccountBalance
    }
}

fun poiTint(typeId: String): Color {
    return when (typeId) {
        "food" -> Color(0xFFFF8F00)
        "student_space" -> Color(0xFF1565C0)
        else -> Color(0xFF2E7D32)
    }
}


