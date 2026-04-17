package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import module.mobile.app.R

@Composable
fun MapTopBar(onToggleTools: () -> Unit, onBack: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo_hits),
            contentDescription = null,
            modifier = Modifier
                .padding(8.dp, top = 25.dp)
                .size(60.dp)
                .align(Alignment.CenterStart)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 72.dp, top = 45.dp)
        ) {
            Button(
                onClick = onToggleTools,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, colorScheme.outline),
                modifier = Modifier.size(45.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.settings_icon),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.secondaryContainer),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 13.dp, top = 45.dp)
                .size(45.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(2.dp, colorScheme.outline),
            contentPadding = PaddingValues(0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.back_home),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

