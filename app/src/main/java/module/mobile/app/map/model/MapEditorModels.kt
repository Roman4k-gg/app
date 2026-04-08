package module.mobile.app.map.model

const val MATRIX_FILE_NAME = "matrix_325_200.txt"
const val POI_FILE_NAME = "poi_points.json"
const val MATRIX_EXPORT_FILE_NAME = "matrix_325_200_edited.txt"
const val POI_EXPORT_FILE_NAME = "poi_points_edited.json"

enum class EditorMode {
    None,
    DrawOne,
    DrawZero,
    AddPoi
}

data class FoodBonus(
    val menu: List<String>,
    val open: String,
    val close: String
)

data class StudentSpaceBonus(
    val capacity: Int,
    val comfort: Float
)

data class PoiItem(
    val id: String,
    val name: String,
    val typeId: String,
    val row: Int,
    val col: Int,
    val foodBonus: FoodBonus? = null,
    val spaceBonus: StudentSpaceBonus? = null
)


