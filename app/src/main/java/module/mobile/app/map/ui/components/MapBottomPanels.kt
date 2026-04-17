package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import module.mobile.app.R

@Composable
fun MapZoomControls(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onZoomIn,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF1F9FF),
                contentColor = Color.Black
            ),
            border = BorderStroke(2.dp, Color(0xFF0072BC)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("+", fontSize = 22.sp)
        }
        Button(
            onClick = onZoomOut,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF1F9FF),
                contentColor = Color.Black
            ),
            border = BorderStroke(2.dp, Color(0xFF0072BC)),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("-", fontSize = 24.sp)
        }
    }
}

@Composable
fun MapBottomActionBar(onToggleSheet: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 25.dp)
            .padding(bottom = 20.dp)
            .height(100.dp),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 20.dp,
        border = BorderStroke(width = 2.dp, Color(0xFF0072BC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Выберите действие",
                fontFamily = FontFamily(Font(R.font.manropebold)),
                fontSize = 22.sp
            )
            Button(
                onClick = onToggleSheet,
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5398F9)),
                border = BorderStroke(2.dp, Color(0xFF0072BC))
            ) {}
        }
    }
}

@Composable
fun MapBottomActionSheet(
    scrollState: ScrollState,
    isRouteMode: Boolean,
    onToggleRouteMode: () -> Unit,
    onOpenClusteringMenu: () -> Unit,
    onOpenGeneticMenu: () -> Unit,
    onOpenAntLandmarks: () -> Unit,
    onOpenAntCowork: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 20.dp,
        border = BorderStroke(width = 2.dp, Color(0xFF0072BC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Выберите действие",
                fontFamily = FontFamily(Font(R.font.manropebold)),
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ActionButton(
                text = "Построить маршрут",
                active = isRouteMode,
                onClick = onToggleRouteMode
            )
            ActionButton(text = "Заведения", onClick = {})
            ActionButton(text = "Кластеры еды", onClick = onOpenClusteringMenu)
            ActionButton(text = "Генетический маршрут еды", onClick = onOpenGeneticMenu)
            ActionButton(text = "Муравьиный тур по роще", onClick = onOpenAntLandmarks)
            ActionButton(text = "Муравьиный подбор коворкинга", onClick = onOpenAntCowork)
            ActionButton(text = "Дерево решений", onClick = {})
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
