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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import module.mobile.app.R

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

    val featureLabels = mapOf(
        "location" to stringResource(R.string.decision_feature_location),
        "budget" to stringResource(R.string.decision_feature_budget),
        "time_available" to stringResource(R.string.decision_feature_time_available),
        "food_type" to stringResource(R.string.decision_feature_food_type),
        "queue_tolerance" to stringResource(R.string.decision_feature_queue_tolerance),
        "weather" to stringResource(R.string.decision_feature_weather)
    )
    val optionLabels = mapOf(
        "main_building" to stringResource(R.string.decision_option_main_building),
        "second_building" to stringResource(R.string.decision_option_second_building),
        "bus_stop" to stringResource(R.string.decision_option_bus_stop),
        "campus_center" to stringResource(R.string.decision_option_campus_center),
        "low" to stringResource(R.string.decision_option_low),
        "medium" to stringResource(R.string.decision_option_medium),
        "high" to stringResource(R.string.decision_option_high),
        "very_short" to stringResource(R.string.decision_option_very_short),
        "short" to stringResource(R.string.decision_option_short),
        "coffee" to stringResource(R.string.decision_option_coffee),
        "pancakes" to stringResource(R.string.decision_option_pancakes),
        "full_meal" to stringResource(R.string.decision_option_full_meal),
        "snack" to stringResource(R.string.decision_option_snack),
        "good" to stringResource(R.string.decision_option_good_weather),
        "bad" to stringResource(R.string.decision_option_bad_weather)
    )

    LaunchedEffect(Unit) {
        algorithm.train(DecisionTreeLunchAlgorithm.DEFAULT_DATA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.decision_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    TextButton(onClick = { showTree = !showTree }) {
                        Text(
                            if (showTree) {
                                stringResource(R.string.decision_hide_tree)
                            } else {
                                stringResource(R.string.decision_show_tree)
                            }
                        )
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
                Text(stringResource(R.string.decision_tree_structure), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                TreeVisualizer(
                    node = algorithm.getRoot(),
                    depth = 0,
                    featureLabels = featureLabels,
                    optionLabels = optionLabels
                )
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(stringResource(R.string.decision_choose_conditions), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            DropdownSelector(
                label = stringResource(R.string.decision_label_location),
                selected = location,
                options = locations,
                optionLabel = { optionLabels[it] ?: it }
            ) { location = it }
            DropdownSelector(
                label = stringResource(R.string.decision_label_budget),
                selected = budget,
                options = budgets,
                optionLabel = { optionLabels[it] ?: it }
            ) { budget = it }
            DropdownSelector(
                label = stringResource(R.string.decision_label_time_available),
                selected = timeAvailable,
                options = times,
                optionLabel = { optionLabels[it] ?: it }
            ) { timeAvailable = it }
            DropdownSelector(
                label = stringResource(R.string.decision_label_food_type),
                selected = foodType,
                options = foods,
                optionLabel = { optionLabels[it] ?: it }
            ) { foodType = it }
            DropdownSelector(
                label = stringResource(R.string.decision_label_queue_tolerance),
                selected = queueTolerance,
                options = tolerances,
                optionLabel = { optionLabels[it] ?: it }
            ) { queueTolerance = it }
            DropdownSelector(
                label = stringResource(R.string.decision_label_weather),
                selected = weather,
                options = weathers,
                optionLabel = { optionLabels[it] ?: it }
            ) { weather = it }

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
                Text(stringResource(R.string.decision_find_lunch_place), fontSize = 16.sp, modifier = Modifier.padding(8.dp))
            }

            result?.let { (place, path) ->
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.decision_recommendation), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
                        Text(formatDecisionResult(place), fontSize = 24.sp, color = Color(0xFF1976D2), fontWeight = FontWeight.ExtraBold)

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(R.string.decision_logic_path), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        path.forEach { step ->
                            Text(
                                stringResource(
                                    R.string.decision_path_item,
                                    formatDecisionStep(step, featureLabels, optionLabels)
                                ),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(stringResource(R.string.decision_csv_training_title), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.decision_csv_training_hint),
                fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
            
            OutlinedTextField(
                value = csvText,
                onValueChange = { csvText = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text(stringResource(R.string.decision_csv_placeholder)) },
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
                    Text(stringResource(R.string.decision_train))
                }
                
                OutlinedButton(
                    onClick = {
                        algorithm.train(DecisionTreeLunchAlgorithm.DEFAULT_DATA)
                        result = null
                        csvText = ""
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.decision_reset))
                }
            }
        }
    }
}

@Composable
fun TreeVisualizer(
    node: DecisionNode?,
    depth: Int,
    featureLabels: Map<String, String>,
    optionLabels: Map<String, String>
) {
    if (node == null) return

    Column(modifier = Modifier.padding(start = (depth * 16).dp)) {
        when (node) {
            is DecisionNode.Leaf -> {
                Text(
                    stringResource(R.string.decision_tree_leaf, formatDecisionResult(node.result)),
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Bold
                )
            }
            is DecisionNode.Internal -> {
                Text(
                    stringResource(R.string.decision_tree_if_feature, featureLabels[node.feature] ?: node.feature),
                    fontWeight = FontWeight.Medium
                )
                node.children.forEach { (value, child) ->
                    Text(
                        stringResource(R.string.decision_tree_value_item, optionLabels[value] ?: value),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    TreeVisualizer(child, depth + 1, featureLabels, optionLabels)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    selected: String,
    options: List<String>,
    optionLabel: (String) -> String = { it },
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = optionLabel(selected),
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
                        text = { Text(optionLabel(option)) },
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

private fun formatDecisionResult(place: String): String = place.replace('_', ' ')

private fun formatDecisionStep(
    rawStep: String,
    featureLabels: Map<String, String>,
    optionLabels: Map<String, String>
): String {
    val parts = rawStep.split("=", limit = 2)
    if (parts.size != 2) return rawStep
    val featureKey = parts[0].trim()
    val optionKey = parts[1].trim()
    val featureLabel = featureLabels[featureKey] ?: featureKey
    val optionLabel = optionLabels[optionKey] ?: optionKey
    return "$featureLabel = $optionLabel"
}

