package module.mobile.app.map.data

import android.content.Context
import module.mobile.app.map.model.FoodBonus
import module.mobile.app.map.model.MATRIX_EXPORT_FILE_NAME
import module.mobile.app.map.model.MATRIX_FILE_NAME
import module.mobile.app.map.model.POI_EXPORT_FILE_NAME
import module.mobile.app.map.model.POI_FILE_NAME
import module.mobile.app.map.model.PoiItem
import module.mobile.app.map.model.StudentSpaceBonus
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

fun loadMatrix(context: Context, matrixRows: Int, matrixCols: Int): Array<IntArray> {
    return try {
        val file = File(context.filesDir, MATRIX_FILE_NAME)
        val content = if (file.exists()) {
            file.readText()
        } else {
            context.assets.open(MATRIX_FILE_NAME).bufferedReader().use { it.readText() }
        }

        content
            .lineSequence()
            .filter { it.isNotBlank() }
            .map { line -> line.trim().split(Regex("\\s+")).map { it.toInt() }.toIntArray() }
            .toList()
            .let { lines ->
                if (lines.size == matrixRows && lines.all { it.size == matrixCols }) {
                    lines.toTypedArray()
                } else {
                    Array(matrixRows) { IntArray(matrixCols) }
                }
            }
    } catch (_: Exception) {
        Array(matrixRows) { IntArray(matrixCols) }
    }
}

fun saveMatrix(context: Context, grid: Array<IntArray>) {
    val file = File(context.filesDir, MATRIX_FILE_NAME)
    val text = buildString {
        grid.forEachIndexed { index, row ->
            append(row.joinToString(" "))
            if (index != grid.lastIndex) appendLine()
        }
    }
    file.writeText(text)
}

fun loadPois(context: Context, matrixRows: Int, matrixCols: Int): Pair<Int, List<PoiItem>> {
    return try {
        val file = File(context.filesDir, POI_FILE_NAME)
        val content = if (file.exists()) {
            file.readText()
        } else {
            context.assets.open(POI_FILE_NAME).bufferedReader().use { it.readText() }
        }
        val root = JSONObject(content)
        val schema = root.optInt("schemaVersion", 5)
        val pois = root.optJSONArray("pois") ?: JSONArray()
        val result = mutableListOf<PoiItem>()

        for (i in 0 until pois.length()) {
            val obj = pois.getJSONObject(i)
            val anchor = obj.getJSONObject("anchor")
            val row = anchor.getInt("row")
            val col = anchor.getInt("col")
            if (row !in 0 until matrixRows || col !in 0 until matrixCols) continue

            val typeId = obj.optString("typeId", "landmark")
            val foodBonus = if (typeId == "food" && obj.has("geneticBonus")) {
                val genetic = obj.getJSONObject("geneticBonus")
                val menu = mutableListOf<String>()
                val menuArray = genetic.optJSONArray("menu") ?: JSONArray()
                for (j in 0 until menuArray.length()) {
                    menu.add(menuArray.getString(j))
                }
                val wh = genetic.optJSONObject("workingHours")
                FoodBonus(
                    menu = menu,
                    open = wh?.optString("open", "09:00") ?: "09:00",
                    close = wh?.optString("close", "18:00") ?: "18:00"
                )
            } else {
                null
            }

            val spaceBonus = if (typeId == "student_space" && obj.has("antBonus")) {
                val ant = obj.getJSONObject("antBonus")
                StudentSpaceBonus(
                    capacity = ant.optInt("capacity", 20),
                    comfort = ant.optDouble("comfort", 0.5).toFloat()
                )
            } else {
                null
            }

            result.add(
                PoiItem(
                    id = obj.optString("id", "poi_${i + 1}"),
                    name = obj.optString("name", "poi_${i + 1}"),
                    typeId = typeId,
                    row = row,
                    col = col,
                    foodBonus = foodBonus,
                    spaceBonus = spaceBonus
                )
            )
        }

        Pair(schema, result)
    } catch (_: Exception) {
        Pair(5, emptyList())
    }
}

fun savePois(context: Context, schemaVersion: Int, pois: List<PoiItem>) {
    File(context.filesDir, POI_FILE_NAME).writeText(buildPoiJson(schemaVersion, pois).toString(2))
}

fun saveEditorExports(
    context: Context,
    schemaVersion: Int,
    grid: Array<IntArray>,
    pois: List<PoiItem>
) {
    val matrixText = buildString {
        grid.forEachIndexed { index, row ->
            append(row.joinToString(" "))
            if (index != grid.lastIndex) appendLine()
        }
    }
    File(context.filesDir, MATRIX_EXPORT_FILE_NAME).writeText(matrixText)
    File(context.filesDir, POI_EXPORT_FILE_NAME).writeText(buildPoiJson(schemaVersion, pois).toString(2))
}

private fun buildPoiJson(schemaVersion: Int, pois: List<PoiItem>): JSONObject {
    val root = JSONObject()
    root.put("schemaVersion", schemaVersion)
    val array = JSONArray()

    pois.forEach { poi ->
        val obj = JSONObject()
        obj.put("id", poi.id)
        obj.put("name", poi.name)
        obj.put("typeId", poi.typeId)
        obj.put("anchor", JSONObject().put("row", poi.row).put("col", poi.col))

        if (poi.typeId == "food") {
            val food = poi.foodBonus ?: FoodBonus(emptyList(), "09:00", "18:00")
            val menuArray = JSONArray()
            food.menu.forEach { menuArray.put(it) }
            obj.put(
                "geneticBonus",
                JSONObject()
                    .put("menu", menuArray)
                    .put("workingHours", JSONObject().put("open", food.open).put("close", food.close))
            )
        }

        if (poi.typeId == "student_space") {
            val space = poi.spaceBonus ?: StudentSpaceBonus(20, 0.5f)
            obj.put(
                "antBonus",
                JSONObject()
                    .put("capacity", space.capacity)
                    .put("comfort", space.comfort)
            )
        }

        array.put(obj)
    }

    root.put("pois", array)
    return root
}

fun nextPoiId(existing: List<PoiItem>, typeId: String): String {
    val prefix = when (typeId) {
        "food" -> "poi_food_"
        "student_space" -> "poi_space_"
        else -> "poi_landmark_"
    }
    val max = existing
        .mapNotNull { item ->
            if (!item.id.startsWith(prefix)) return@mapNotNull null
            item.id.removePrefix(prefix).toIntOrNull()
        }
        .maxOrNull() ?: 0
    return prefix + (max + 1).toString().padStart(3, '0')
}


