package module.mobile.app.map.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
fun GeneticFoodMenuSheet(
    menuItems: List<String>,
    cartCount: Int,
    onAddToCart: (String) -> Unit,
    onOpenCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp),
        color = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 20.dp,
        border = BorderStroke(width = 2.dp, Color(0xFF0072BC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.genetic_choose_dishes),
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 22.sp
                )
                Button(
                    onClick = onOpenCart,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5398F9)),
                    border = BorderStroke(2.dp, Color(0xFF0072BC))
                ) {
                    Text(text = stringResource(R.string.genetic_cart_button, cartCount))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(menuItems, key = { it }) { item ->
                    OutlinedButton(
                        onClick = { onAddToCart(item) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, Color(0xFF0072BC)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item, fontSize = 15.sp)
                            Text(text = "+", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneticCartDialog(
    cartItems: List<Pair<String, Int>>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onBuy: () -> Unit,
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
                    .fillMaxWidth()
                    .height(460.dp)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.genetic_cart_title),
                    fontFamily = FontFamily(Font(R.font.manropebold)),
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (cartItems.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.genetic_cart_empty), color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cartItems, key = { it.first }) { (name, count) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF0072BC)),
                                color = Color.White
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "-",
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .clickable { onRemove(name) }
                                                .padding(horizontal = 10.dp)
                                        )
                                        Text(text = count.toString(), fontSize = 16.sp)
                                        Text(
                                            text = "+",
                                            fontSize = 22.sp,
                                            modifier = Modifier
                                                .clickable { onAdd(name) }
                                                .padding(horizontal = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClear, enabled = cartItems.isNotEmpty()) {
                        Text(stringResource(R.string.common_clear))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.common_close))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onBuy,
                            enabled = cartItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF5398F9),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(2.dp, Color(0xFF0072BC))
                        ) {
                            Text(stringResource(R.string.common_buy))
                        }
                    }
                }
            }
        }
    }
}

