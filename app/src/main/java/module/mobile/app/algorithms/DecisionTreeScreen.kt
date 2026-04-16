package module.mobile.app.algorithms

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecisionTreeScreen(onBack: () -> Unit) {
    val algorithm = remember { DecisionTreeLunchAlgorithm() }
    var csvText by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Pair<String, List<String>>?>(null) }
    var showTree by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf("main_building") }
    var budget by remember { mutableStateOf("low") }
    var timeAvailable by remember { mutableStateOf("medium") }
    var foodType by remember { mutableStateOf("full_meal") }
    var queueTolerance by remember { mutableStateOf("medium") }
    var weather by remember { mutableStateOf("good") }

    val locations = listOf("main_building", "second_building", "bus_stop", "campus_center")
    val budgets = listOf("low", "medium", "high")
    val times = listOf("very_short", "short", "medium")
    val foods = listOf("coffee", "pancakes", "full_meal", "snack")
    val tolerances = listOf("low", "medium", "high")
    val weathers = listOf("good", "bad")

    LaunchedEffect(Unit) {
        algorithm.train(DecisionTreeLunchAlgorithm.DEFAULT_DATA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Где пообедать?") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { showTree = !showTree }) {
                        Text(if (showTree) "Скрыть дерево" else "Показать дерево")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showTree) {
                Text("Структура дерева решений:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TreeVisualizer(node = algorithm.getRoot(), depth = 0)
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text("Выберите условия:", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector("Местоположение", location, locations) { location = it }
            DropdownSelector("Бюджет", budget, budgets) { budget = it }
            DropdownSelector("Доступное время", timeAvailable, times) { timeAvailable = it }
            DropdownSelector("Тип еды", foodType, foods) { foodType = it }
            DropdownSelector("Очередь", queueTolerance, tolerances) { queueTolerance = it }
            DropdownSelector("Погода", weather, weathers) { weather = it }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val input = mapOf(
                        "location" to location,
                        "budget" to budget,
                        "time_available" to timeAvailable,
                        "food_type" to foodType,
                        "queue_tolerance" to queueTolerance,
                        "weather" to weather
                    )
                    result = algorithm.predict(input)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Найти место для обеда", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }

            result?.let { (place, path) ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Рекомендация:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                        Text(place, fontSize = 24.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.ExtraBold)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Логика выбора (путь в дереве):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        path.forEach { step ->
                            Text("• $step", fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, top = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Обучение на CSV:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Загрузите свои данные в формате: location,budget,time_available,food_type,queue_tolerance,weather,recommended_place", 
                fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            OutlinedTextField(
                value = csvText,
                onValueChange = { csvText = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Вставьте данные...") },
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
            
            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val customData = DecisionTreeLunchAlgorithm.parseCsv(csvText)
                        if (customData.isNotEmpty()) {
                            algorithm.train(customData)
                            result = null
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Обучить")
                }
                
                OutlinedButton(
                    onClick = {
                        algorithm.train(DecisionTreeLunchAlgorithm.DEFAULT_DATA)
                        result = null
                        csvText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сбросить")
                }
            }
        }
    }
}

@Composable
fun TreeVisualizer(node: DecisionNode?, depth: Int) {
    if (node == null) return

    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        when (node) {
            is DecisionNode.Leaf -> {
                Text("↳ Решение: ${node.result}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
            }
            is DecisionNode.Internal -> {
                Text("? Если ${node.feature}:", fontWeight = FontWeight.Medium)
                node.children.forEach { (value, child) ->
                    Text("  - $value:", color = Color.Gray, fontSize = 12.sp)
                    TreeVisualizer(child, depth + 1)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, selected: String, options: List<String>, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
