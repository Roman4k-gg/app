package module.mobile.app.algorithms

import kotlin.math.log2

data class TrainingExample(
    val features: Map<String, String>,
    val target: String
)

sealed class DecisionNode {
    data class Leaf(val result: String) : DecisionNode()
    data class Internal(
        val feature: String,
        val children: Map<String, DecisionNode>,
        val defaultResult: String
    ) : DecisionNode()
}

class DecisionTreeLunchAlgorithm {

    private var root: DecisionNode? = null
    private var features: List<String> = emptyList()

    fun train(examples: List<TrainingExample>) {
        if (examples.isEmpty()) return
        features = examples.first().features.keys.toList()
        root = buildTree(examples, features)
    }

    private fun buildTree(examples: List<TrainingExample>, availableFeatures: List<String>): DecisionNode {
        val targets = examples.map { it.target }
        if (targets.distinct().size == 1) {
            return DecisionNode.Leaf(targets.first())
        }
        if (availableFeatures.isEmpty()) {
            return DecisionNode.Leaf(targets.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Unknown")
        }

        val bestFeature = availableFeatures.maxByOrNull { calculateInformationGain(examples, it) } ?: return DecisionNode.Leaf(targets.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Unknown")
        
        val remainingFeatures = availableFeatures - bestFeature
        val children = examples.groupBy { it.features[bestFeature]!! }
            .mapValues { (_, subset) -> buildTree(subset, remainingFeatures) }

        val mostCommonTarget = targets.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "Unknown"
        
        return DecisionNode.Internal(bestFeature, children, mostCommonTarget)
    }

    private fun calculateEntropy(examples: List<TrainingExample>): Double {
        val counts = examples.map { it.target }.groupBy { it }.mapValues { it.value.size }
        val total = examples.size.toDouble()
        return counts.values.sumOf { count ->
            val p = count / total
            -p * log2(p)
        }
    }

    private fun calculateInformationGain(examples: List<TrainingExample>, feature: String): Double {
        val baseEntropy = calculateEntropy(examples)
        val subsets = examples.groupBy { it.features[feature]!! }
        val total = examples.size.toDouble()
        
        val featureEntropy = subsets.values.sumOf { subset ->
            (subset.size / total) * calculateEntropy(subset)
        }
        
        return baseEntropy - featureEntropy
    }

    fun predict(features: Map<String, String>): Pair<String, List<String>> {
        val path = mutableListOf<String>()
        var currentNode = root ?: return "Not trained" to path
        
        while (currentNode is DecisionNode.Internal) {
            val value = features[currentNode.feature]
            path.add("${currentNode.feature} = $value")
            currentNode = currentNode.children[value] ?: return currentNode.defaultResult to path
        }
        
        if (currentNode is DecisionNode.Leaf) {
            return currentNode.result to path
        }
        
        return "Unknown" to path
    }

    fun getRoot() = root

    companion object {
        val DEFAULT_DATA = listOf(
            TrainingExample(mapOf("location" to "main_building", "budget" to "low", "time_available" to "medium", "food_type" to "full_meal", "queue_tolerance" to "medium", "weather" to "good"), "Main_Cafeteria"),
            TrainingExample(mapOf("location" to "main_building", "budget" to "low", "time_available" to "short", "food_type" to "snack", "queue_tolerance" to "low", "weather" to "good"), "Yarche"),
            TrainingExample(mapOf("location" to "main_building", "budget" to "medium", "time_available" to "short", "food_type" to "coffee", "queue_tolerance" to "low", "weather" to "good"), "Bus_Stop_Coffee"),
            TrainingExample(mapOf("location" to "main_building", "budget" to "high", "time_available" to "medium", "food_type" to "coffee", "queue_tolerance" to "medium", "weather" to "good"), "Starbooks"),
            TrainingExample(mapOf("location" to "second_building", "budget" to "low", "time_available" to "very_short", "food_type" to "snack", "queue_tolerance" to "low", "weather" to "good"), "Vending_Machine"),
            TrainingExample(mapOf("location" to "second_building", "budget" to "medium", "time_available" to "short", "food_type" to "coffee", "queue_tolerance" to "medium", "weather" to "good"), "Second_Building_Cafe"),
            TrainingExample(mapOf("location" to "second_building", "budget" to "medium", "time_available" to "medium", "food_type" to "full_meal", "queue_tolerance" to "medium", "weather" to "good"), "Main_Cafeteria"),
            TrainingExample(mapOf("location" to "second_building", "budget" to "low", "time_available" to "short", "food_type" to "snack", "queue_tolerance" to "low", "weather" to "bad"), "Vending_Machine"),
            TrainingExample(mapOf("location" to "campus_center", "budget" to "medium", "time_available" to "short", "food_type" to "pancakes", "queue_tolerance" to "medium", "weather" to "good"), "Siberian_Pancakes")
        )
        
        fun parseCsv(csv: String): List<TrainingExample> {
            val lines = csv.trim().split("\n")
            if (lines.size < 2) return emptyList()
            val header = lines[0].split(",").map { it.trim() }
            val targetIndex = header.indexOf("recommended_place")
            if (targetIndex == -1) return emptyList()
            
            val featuresIndices = header.indices.filter { it != targetIndex }
            
            return lines.drop(1).mapNotNull { line ->
                val values = line.split(",").map { it.trim() }
                if (values.size != header.size) return@mapNotNull null
                
                val features = featuresIndices.associate { i -> header[i] to values[i] }
                TrainingExample(features, values[targetIndex])
            }
        }
    }
}
