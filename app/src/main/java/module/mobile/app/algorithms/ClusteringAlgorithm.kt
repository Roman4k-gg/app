package module.mobile.app.algorithms

import module.mobile.app.map.model.PoiItem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

enum class ClusterMetric {
    Euclidean,
    WalkableAStar
}

data class ClusteringResult(
    val metrics: Set<ClusterMetric>,
    val selectedKByMetric: Map<ClusterMetric, Int>,
    val assignmentsByMetric: Map<ClusterMetric, Map<String, Int>>,
    val zonesByMetric: Map<ClusterMetric, Map<Pair<Int, Int>, Int>>,
    val differingPoiIds: Set<String>,
    val differenceDescriptions: Map<String, String>
)

class ClusteringAlgorithm {

    private data class ClusterRun(
        val k: Int,
        val assignments: IntArray,
        val score: Double
    )

    fun clusterFoodPois(
        grid: Array<IntArray>,
        pois: List<PoiItem>,
        metrics: Set<ClusterMetric>,
        randomSeed: Int = 42
    ): ClusteringResult {
        val foodPois = pois
            .asSequence()
            .filter { it.typeId == "food" }
            .distinctBy { it.id }
            .toList()

        if (foodPois.isEmpty()) {
            return ClusteringResult(
                metrics = emptySet(),
                selectedKByMetric = emptyMap(),
                assignmentsByMetric = emptyMap(),
                zonesByMetric = emptyMap(),
                differingPoiIds = emptySet(),
                differenceDescriptions = emptyMap()
            )
        }

        val selectedMetrics = if (metrics.isEmpty()) setOf(ClusterMetric.Euclidean) else metrics

        val assignments = mutableMapOf<ClusterMetric, Map<String, Int>>()
        val zoneMaps = mutableMapOf<ClusterMetric, Map<Pair<Int, Int>, Int>>()
        val selectedKByMetric = mutableMapOf<ClusterMetric, Int>()

        if (ClusterMetric.Euclidean in selectedMetrics) {
            val run = autoClusterForMetric(
                metric = ClusterMetric.Euclidean,
                grid = grid,
                pois = foodPois,
                seed = randomSeed + 11
            )
            assignments[ClusterMetric.Euclidean] = foodPois.indices.associate { idx ->
                foodPois[idx].id to run.assignments[idx]
            }
            selectedKByMetric[ClusterMetric.Euclidean] = run.k
            zoneMaps[ClusterMetric.Euclidean] = buildEuclideanZoneMap(
                grid = grid,
                pois = foodPois,
                assignments = run.assignments,
                k = run.k
            )
        }

        if (ClusterMetric.WalkableAStar in selectedMetrics) {
            val run = autoClusterForMetric(
                metric = ClusterMetric.WalkableAStar,
                grid = grid,
                pois = foodPois,
                seed = randomSeed + 29
            )
            assignments[ClusterMetric.WalkableAStar] = foodPois.indices.associate { idx ->
                foodPois[idx].id to run.assignments[idx]
            }
            selectedKByMetric[ClusterMetric.WalkableAStar] = run.k
            zoneMaps[ClusterMetric.WalkableAStar] = buildWalkableZoneMap(
                grid = grid,
                pois = foodPois,
                assignments = run.assignments,
                k = run.k
            )
        }

        val differing = mutableSetOf<String>()
        val descriptions = mutableMapOf<String, String>()

        if (assignments.size >= 2) {
            val metricList = assignments.keys.toList()
            val referenceMetric = metricList.first()
            val referenceAssignments = assignments[referenceMetric].orEmpty()

            foodPois.forEach { poi ->
                val values = metricList.mapNotNull { metric ->
                    assignments[metric]?.get(poi.id)?.let { metric to it }
                }
                if (values.isEmpty()) return@forEach

                val different = values.any { it.second != values.first().second }
                if (different) {
                    differing.add(poi.id)
                    descriptions[poi.id] = values.joinToString(" | ") { (metric, clusterId) ->
                        "${metricName(metric)}: C${clusterId + 1}"
                    }
                } else {
                    val refCluster = referenceAssignments[poi.id]
                    if (refCluster != null) {
                        descriptions[poi.id] = "${metricName(referenceMetric)}: C${refCluster + 1}"
                    }
                }
            }
        } else {
            val onlyMetric = assignments.keys.firstOrNull()
            val onlyMap = onlyMetric?.let { assignments[it] }.orEmpty()
            foodPois.forEach { poi ->
                val clusterId = onlyMap[poi.id]
                if (clusterId != null && onlyMetric != null) {
                    descriptions[poi.id] = "${metricName(onlyMetric)}: C${clusterId + 1}"
                }
            }
        }

        return ClusteringResult(
            metrics = selectedMetrics,
            selectedKByMetric = selectedKByMetric,
            assignmentsByMetric = assignments,
            zonesByMetric = zoneMaps,
            differingPoiIds = differing,
            differenceDescriptions = descriptions
        )
    }

    private fun autoClusterForMetric(
        metric: ClusterMetric,
        grid: Array<IntArray>,
        pois: List<PoiItem>,
        seed: Int
    ): ClusterRun {
        val n = pois.size
        if (n == 1) {
            return ClusterRun(k = 1, assignments = intArrayOf(0), score = 0.0)
        }

        val points = pois.map { doubleArrayOf(it.row.toDouble(), it.col.toDouble()) }
        val distanceMatrix = when (metric) {
            ClusterMetric.Euclidean -> buildEuclideanDistanceMatrix(points)
            ClusterMetric.WalkableAStar -> buildWalkableDistanceMatrix(grid, pois)
        }

        val maxK = min(8, n)
        val candidateKs = if (n == 2) listOf(2) else (2..maxK).toList()
        var bestRun: ClusterRun? = null

        candidateKs.forEach { k ->
            val random = Random(seed + k * 31)
            val assignments = when (metric) {
                ClusterMetric.Euclidean -> clusterEuclidean(points, k, random)
                ClusterMetric.WalkableAStar -> clusterWalkable(distanceMatrix, k, random)
            }
            val score = silhouetteScore(assignments, k, distanceMatrix)
            val candidate = ClusterRun(k = k, assignments = assignments, score = score)
            val currentBest = bestRun
            if (currentBest == null || candidate.score > currentBest.score ||
                (candidate.score == currentBest.score && candidate.k < currentBest.k)
            ) {
                bestRun = candidate
            }
        }

        return bestRun ?: ClusterRun(k = 1, assignments = IntArray(n), score = 0.0)
    }

    private fun clusterEuclidean(points: List<DoubleArray>, k: Int, random: Random): IntArray {
        val centroids = points.shuffled(random).take(k).map { it.copyOf() }.toMutableList()
        val assignments = IntArray(points.size)

        repeat(30) {
            var changed = false
            for (i in points.indices) {
                val point = points[i]
                var bestCluster = 0
                var bestDistance = Double.MAX_VALUE
                for (c in centroids.indices) {
                    val d = euclidean(point, centroids[c])
                    if (d < bestDistance) {
                        bestDistance = d
                        bestCluster = c
                    }
                }
                if (assignments[i] != bestCluster) {
                    assignments[i] = bestCluster
                    changed = true
                }
            }

            val sum = Array(k) { doubleArrayOf(0.0, 0.0) }
            val count = IntArray(k)
            for (i in points.indices) {
                val cluster = assignments[i]
                sum[cluster][0] += points[i][0]
                sum[cluster][1] += points[i][1]
                count[cluster] += 1
            }

            for (c in 0 until k) {
                if (count[c] == 0) {
                    val fallback = points[random.nextInt(points.size)]
                    centroids[c][0] = fallback[0]
                    centroids[c][1] = fallback[1]
                } else {
                    centroids[c][0] = sum[c][0] / count[c]
                    centroids[c][1] = sum[c][1] / count[c]
                }
            }

            if (!changed) return@repeat
        }

        return assignments
    }

    private fun clusterWalkable(distanceMatrix: Array<DoubleArray>, k: Int, random: Random): IntArray {
        val n = distanceMatrix.size
        val medoids = (0 until n).shuffled(random).take(k).toMutableList()
        val assignments = IntArray(n)

        repeat(25) {
            for (i in 0 until n) {
                var bestCluster = 0
                var bestDistance = Double.MAX_VALUE
                for (c in medoids.indices) {
                    val d = distanceMatrix[i][medoids[c]]
                    if (d < bestDistance) {
                        bestDistance = d
                        bestCluster = c
                    }
                }
                assignments[i] = bestCluster
            }

            var changed = false
            for (cluster in 0 until k) {
                val members = (0 until n).filter { assignments[it] == cluster }
                if (members.isEmpty()) continue

                var bestMedoid = medoids[cluster]
                var bestCost = Double.MAX_VALUE
                members.forEach { candidate ->
                    var cost = 0.0
                    members.forEach { member ->
                        cost += distanceMatrix[candidate][member]
                    }
                    if (cost < bestCost) {
                        bestCost = cost
                        bestMedoid = candidate
                    }
                }

                if (bestMedoid != medoids[cluster]) {
                    medoids[cluster] = bestMedoid
                    changed = true
                }
            }

            if (!changed) return@repeat
        }

        return assignments
    }

    private fun buildEuclideanDistanceMatrix(points: List<DoubleArray>): Array<DoubleArray> {
        val n = points.size
        val matrix = Array(n) { DoubleArray(n) { 0.0 } }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = euclidean(points[i], points[j])
                matrix[i][j] = d
                matrix[j][i] = d
            }
        }
        return matrix
    }

    private fun buildWalkableDistanceMatrix(
        grid: Array<IntArray>,
        pois: List<PoiItem>
    ): Array<DoubleArray> {
        val n = pois.size
        val matrix = Array(n) { DoubleArray(n) { 0.0 } }
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val a = pois[i].row to pois[i].col
                val b = pois[j].row to pois[j].col
                val path = astar(grid, a, b)
                val d = if (path == null) 1_000_000.0 else (path.size - 1).toDouble()
                matrix[i][j] = d
                matrix[j][i] = d
            }
        }
        return matrix
    }

    private fun silhouetteScore(
        assignments: IntArray,
        k: Int,
        distances: Array<DoubleArray>
    ): Double {
        if (assignments.isEmpty() || k <= 1) return 0.0

        val clusters = Array(k) { mutableListOf<Int>() }
        assignments.forEachIndexed { idx, cluster ->
            if (cluster in 0 until k) clusters[cluster].add(idx)
        }
        if (clusters.count { it.isNotEmpty() } <= 1) return -1.0

        var total = 0.0
        var counted = 0

        for (i in assignments.indices) {
            val own = assignments[i]
            val ownMembers = clusters[own]
            if (ownMembers.size <= 1) {
                counted += 1
                continue
            }

            var a = 0.0
            ownMembers.forEach { j ->
                if (j != i) a += distances[i][j]
            }
            a /= (ownMembers.size - 1)

            var b = Double.MAX_VALUE
            for (clusterIdx in 0 until k) {
                if (clusterIdx == own || clusters[clusterIdx].isEmpty()) continue
                var avg = 0.0
                clusters[clusterIdx].forEach { j ->
                    avg += distances[i][j]
                }
                avg /= clusters[clusterIdx].size
                if (avg < b) b = avg
            }

            val denom = max(a, b)
            val s = if (denom <= 0.0 || b == Double.MAX_VALUE) 0.0 else (b - a) / denom
            total += s
            counted += 1
        }

        return if (counted == 0) 0.0 else total / counted
    }

    private fun buildEuclideanZoneMap(
        grid: Array<IntArray>,
        pois: List<PoiItem>,
        assignments: IntArray,
        k: Int
    ): Map<Pair<Int, Int>, Int> {
        return buildHullZoneMap(
            grid = grid,
            pois = pois,
            assignments = assignments,
            k = k,
            walkableOnly = false
        )
    }

    private fun buildWalkableZoneMap(
        grid: Array<IntArray>,
        pois: List<PoiItem>,
        assignments: IntArray,
        k: Int
    ): Map<Pair<Int, Int>, Int> {
        if (pois.isEmpty() || k <= 0) return emptyMap()

        val rows = grid.size
        val cols = grid.firstOrNull()?.size ?: return emptyMap()
        val zoneMap = mutableMapOf<Pair<Int, Int>, Int>()

        for (cluster in 0 until k) {
            val memberPoints = pois.indices
                .filter { assignments[it] == cluster }
                .mapNotNull { idx -> nearestWalkableCell(grid, pois[idx].row to pois[idx].col) }
                .distinct()

            if (memberPoints.isEmpty()) continue

            val clusterCells = mutableSetOf<Pair<Int, Int>>()
            memberPoints.forEach { seed ->
                if (seed.first in 0 until rows && seed.second in 0 until cols && grid[seed.first][seed.second] == 1) {
                    clusterCells.add(seed)
                }
            }

            if (memberPoints.size >= 2) {
                val pathCache = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>?>()

                fun getPath(i: Int, j: Int): List<Pair<Int, Int>>? {
                    val a = min(i, j)
                    val b = max(i, j)
                    val key = a to b
                    val cached = pathCache[key]
                    if (cached != null || key in pathCache) return cached
                    val path = astar(grid, memberPoints[a], memberPoints[b])
                    pathCache[key] = path
                    return path
                }

                // Соединяем точки кластера минимальным набором кратчайших путей (MST), чтобы зона была непрерывной.
                val inTree = BooleanArray(memberPoints.size)
                inTree[0] = true

                repeat(memberPoints.size - 1) {
                    var bestFrom = -1
                    var bestTo = -1
                    var bestLength = Int.MAX_VALUE
                    var bestPath: List<Pair<Int, Int>>? = null

                    for (i in memberPoints.indices) {
                        if (!inTree[i]) continue
                        for (j in memberPoints.indices) {
                            if (inTree[j]) continue
                            val path = getPath(i, j) ?: continue
                            if (path.size < bestLength) {
                                bestLength = path.size
                                bestFrom = i
                                bestTo = j
                                bestPath = path
                            }
                        }
                    }

                    if (bestTo == -1 || bestFrom == -1 || bestPath == null) return@repeat

                    inTree[bestTo] = true
                    bestPath.forEach { cell ->
                        val (r, c) = cell
                        if (r in 0 until rows && c in 0 until cols && grid[r][c] == 1) {
                            clusterCells.add(cell)
                        }
                    }
                }
            }

            val smoothed = dilateWalkableCells(grid, clusterCells, steps = 1)
            smoothed.forEach { cell ->
                val (r, c) = cell
                if (r !in 0 until rows || c !in 0 until cols || grid[r][c] != 1) return@forEach
                zoneMap.putIfAbsent(cell, cluster)
            }
        }

        return zoneMap
    }

    private fun dilateWalkableCells(
        grid: Array<IntArray>,
        source: Set<Pair<Int, Int>>,
        steps: Int
    ): Set<Pair<Int, Int>> {
        if (source.isEmpty() || steps <= 0) return source

        val rows = grid.size
        val cols = grid.firstOrNull()?.size ?: return source
        val result = source.toMutableSet()
        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)

        repeat(steps) {
            val frontier = result.toList()
            frontier.forEach { (r, c) ->
                for (i in 0 until 4) {
                    val nr = r + dr[i]
                    val nc = c + dc[i]
                    if (nr !in 0 until rows || nc !in 0 until cols) continue
                    if (grid[nr][nc] == 1) result.add(nr to nc)
                }
            }
        }

        return result
    }

    private fun nearestWalkableCell(
        grid: Array<IntArray>,
        start: Pair<Int, Int>
    ): Pair<Int, Int>? {
        val rows = grid.size
        val cols = grid.firstOrNull()?.size ?: return null
        val (sr, sc) = start

        if (sr !in 0 until rows || sc !in 0 until cols) return null
        if (grid[sr][sc] == 1) return start

        val visited = Array(rows) { BooleanArray(cols) }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addLast(start)
        visited[sr][sc] = true

        val dr = intArrayOf(-1, 1, 0, 0)
        val dc = intArrayOf(0, 0, -1, 1)

        while (queue.isNotEmpty()) {
            val (row, col) = queue.removeFirst()
            for (i in 0 until 4) {
                val nr = row + dr[i]
                val nc = col + dc[i]
                if (nr !in 0 until rows || nc !in 0 until cols) continue
                if (visited[nr][nc]) continue
                if (grid[nr][nc] == 1) return nr to nc
                visited[nr][nc] = true
                queue.addLast(nr to nc)
            }
        }

        return null
    }

    private fun buildHullZoneMap(
        grid: Array<IntArray>,
        pois: List<PoiItem>,
        assignments: IntArray,
        k: Int,
        walkableOnly: Boolean
    ): Map<Pair<Int, Int>, Int> {
        if (pois.isEmpty() || k <= 0) return emptyMap()
        val rows = grid.size
        val cols = grid.firstOrNull()?.size ?: return emptyMap()
        val zoneMap = mutableMapOf<Pair<Int, Int>, Int>()

        for (cluster in 0 until k) {
            val clusterPoints = pois.indices
                .filter { assignments[it] == cluster }
                .map { pois[it].row to pois[it].col }

            if (clusterPoints.isEmpty()) continue

            val candidateCells = when (clusterPoints.size) {
                1 -> buildSinglePointArea(clusterPoints.first(), rows, cols)
                2 -> buildTwoPointArea(clusterPoints[0], clusterPoints[1], rows, cols)
                else -> buildPolygonArea(clusterPoints, rows, cols)
            }

            candidateCells.forEach { cell ->
                val (r, c) = cell
                if (r !in 0 until rows || c !in 0 until cols) return@forEach
                if (walkableOnly && grid[r][c] != 1) return@forEach
                zoneMap[cell] = cluster
            }
        }

        return zoneMap
    }

    private fun buildSinglePointArea(
        point: Pair<Int, Int>,
        rows: Int,
        cols: Int
    ): Set<Pair<Int, Int>> {
        val radius = 6
        val (pr, pc) = point
        val result = mutableSetOf<Pair<Int, Int>>()

        for (r in max(0, pr - radius)..min(rows - 1, pr + radius)) {
            for (c in max(0, pc - radius)..min(cols - 1, pc + radius)) {
                val dr = r - pr
                val dc = c - pc
                if (dr * dr + dc * dc <= radius * radius) {
                    result.add(r to c)
                }
            }
        }
        return result
    }

    private fun buildTwoPointArea(
        a: Pair<Int, Int>,
        b: Pair<Int, Int>,
        rows: Int,
        cols: Int
    ): Set<Pair<Int, Int>> {
        val thickness = 4.0
        val (ar, ac) = a
        val (br, bc) = b
        val minR = max(0, min(ar, br) - 6)
        val maxR = min(rows - 1, max(ar, br) + 6)
        val minC = max(0, min(ac, bc) - 6)
        val maxC = min(cols - 1, max(ac, bc) + 6)
        val result = mutableSetOf<Pair<Int, Int>>()

        for (r in minR..maxR) {
            for (c in minC..maxC) {
                val distance = distancePointToSegment(
                    px = r + 0.5,
                    py = c + 0.5,
                    ax = ar + 0.5,
                    ay = ac + 0.5,
                    bx = br + 0.5,
                    by = bc + 0.5
                )
                if (distance <= thickness) {
                    result.add(r to c)
                }
            }
        }
        return result
    }

    private fun buildPolygonArea(
        points: List<Pair<Int, Int>>,
        rows: Int,
        cols: Int
    ): Set<Pair<Int, Int>> {
        val hull = convexHull(points)
        if (hull.size < 3) return buildTwoPointArea(points.first(), points.last(), rows, cols)

        val minR = max(0, hull.minOf { it.first } - 1)
        val maxR = min(rows - 1, hull.maxOf { it.first } + 1)
        val minC = max(0, hull.minOf { it.second } - 1)
        val maxC = min(cols - 1, hull.maxOf { it.second } + 1)
        val result = mutableSetOf<Pair<Int, Int>>()

        for (r in minR..maxR) {
            for (c in minC..maxC) {
                if (isPointInPolygon(r + 0.5, c + 0.5, hull)) {
                    result.add(r to c)
                }
            }
        }

        return result
    }

    private fun convexHull(points: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
        if (points.size <= 2) return points.distinct()
        val sorted = points.distinct().sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
        if (sorted.size <= 2) return sorted

        val lower = mutableListOf<Pair<Int, Int>>()
        sorted.forEach { p ->
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0L) {
                lower.removeAt(lower.lastIndex)
            }
            lower.add(p)
        }

        val upper = mutableListOf<Pair<Int, Int>>()
        for (i in sorted.indices.reversed()) {
            val p = sorted[i]
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0L) {
                upper.removeAt(upper.lastIndex)
            }
            upper.add(p)
        }

        lower.removeAt(lower.lastIndex)
        upper.removeAt(upper.lastIndex)
        return lower + upper
    }

    private fun cross(a: Pair<Int, Int>, b: Pair<Int, Int>, c: Pair<Int, Int>): Long {
        val abx = b.first - a.first
        val aby = b.second - a.second
        val acx = c.first - a.first
        val acy = c.second - a.second
        return abx.toLong() * acy.toLong() - aby.toLong() * acx.toLong()
    }

    private fun isPointInPolygon(
        x: Double,
        y: Double,
        polygon: List<Pair<Int, Int>>
    ): Boolean {
        var inside = false
        var j = polygon.lastIndex

        for (i in polygon.indices) {
            val xi = polygon[i].first + 0.5
            val yi = polygon[i].second + 0.5
            val xj = polygon[j].first + 0.5
            val yj = polygon[j].second + 0.5

            val onEdge = distancePointToSegment(x, y, xi, yi, xj, yj) < 0.45
            if (onEdge) return true

            val intersects = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi) + 1e-9) + xi)
            if (intersects) inside = !inside

            j = i
        }

        return inside
    }

    private fun distancePointToSegment(
        px: Double,
        py: Double,
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double
    ): Double {
        val abx = bx - ax
        val aby = by - ay
        val apx = px - ax
        val apy = py - ay
        val abLenSq = abx * abx + aby * aby

        if (abLenSq <= 1e-9) {
            val dx = px - ax
            val dy = py - ay
            return sqrt(dx * dx + dy * dy)
        }

        val t = ((apx * abx) + (apy * aby)) / abLenSq
        val clampedT = t.coerceIn(0.0, 1.0)
        val cx = ax + clampedT * abx
        val cy = ay + clampedT * aby
        val dx = px - cx
        val dy = py - cy
        return sqrt(dx * dx + dy * dy)
    }

    private fun euclidean(a: DoubleArray, b: DoubleArray): Double {
        val dr = a[0] - b[0]
        val dc = a[1] - b[1]
        return sqrt(dr * dr + dc * dc)
    }

    private fun metricName(metric: ClusterMetric): String {
        return when (metric) {
            ClusterMetric.Euclidean -> "Euclidean"
            ClusterMetric.WalkableAStar -> "Walkable(A*)"
        }
    }
}
