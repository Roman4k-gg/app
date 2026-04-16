package module.mobile.app.algorithms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import module.mobile.app.map.model.PoiItem
import kotlin.math.max
import kotlin.math.pow
import kotlin.random.Random

data class AntTourConfig(
    val iterations: Int = 70,
    val antsPerIteration: Int = 40,
    val alpha: Double = 1.0,
    val beta: Double = 3.0,
    val evaporation: Double = 0.35,
    val q: Double = 220.0,
    val unreachablePenalty: Double = 1_000_000.0,
    val randomSeed: Int? = null
)

data class AntTourRequest(
    val grid: Array<IntArray>,
    val startCell: Pair<Int, Int>,
    val landmarks: List<PoiItem>,
    val config: AntTourConfig = AntTourConfig()
)

data class AntTourProgress(
    val iteration: Int,
    val totalIterations: Int,
    val bestScore: Double,
    val bestVisitOrder: List<PoiItem>,
    val bestPath: List<Pair<Int, Int>>
)

data class AntCoworkConfig(
    val iterations: Int = 70,
    val alpha: Double = 1.0,
    val beta: Double = 2.0,
    val evaporation: Double = 0.30,
    val q: Double = 140.0,
    val comfortWeight: Double = 8.0,
    val overloadPenalty: Double = 400.0,
    val unreachablePenalty: Double = 1_000_000.0,
    val randomSeed: Int? = null
)

data class AntCoworkRequest(
    val grid: Array<IntArray>,
    val startCell: Pair<Int, Int>,
    val studentCount: Int,
    val spaces: List<PoiItem>,
    val config: AntCoworkConfig = AntCoworkConfig()
)

data class AntCoworkAllocation(
    val poi: PoiItem,
    val assignedStudents: Int,
    val capacity: Int,
    val comfort: Float
)

data class AntCoworkProgress(
    val iteration: Int,
    val totalIterations: Int,
    val bestScore: Double,
    val primarySpace: PoiItem?,
    val allocation: List<AntCoworkAllocation>,
    val primaryPath: List<Pair<Int, Int>>
)

class AntColonyAlgorithm {

    fun runLandmarkTour(request: AntTourRequest): Flow<AntTourProgress> = flow {
        val landmarks = request.landmarks
            .asSequence()
            .filter { it.typeId == "landmark" }
            .distinctBy { it.id }
            .toList()

        if (landmarks.isEmpty()) {
            emit(
                AntTourProgress(
                    iteration = 0,
                    totalIterations = request.config.iterations,
                    bestScore = 0.0,
                    bestVisitOrder = emptyList(),
                    bestPath = listOf(request.startCell)
                )
            )
            return@flow
        }

        val random = request.config.randomSeed?.let { Random(it) } ?: Random.Default
        val matrix = buildDistanceMatrix(request.grid, request.startCell, landmarks, request.config.unreachablePenalty)

        val size = landmarks.size + 1
        val pheromone = Array(size) { DoubleArray(size) { 1.0 } }

        var bestScore = Double.MAX_VALUE
        var bestRoute: List<Int> = emptyList()

        emit(
            AntTourProgress(
                iteration = 0,
                totalIterations = request.config.iterations,
                bestScore = bestScore,
                bestVisitOrder = emptyList(),
                bestPath = listOf(request.startCell)
            )
        )

        repeat(request.config.iterations) { iterIndex ->
            val antCount = max(6, request.config.antsPerIteration)
            val iterationRoutes = mutableListOf<Pair<List<Int>, Double>>()

            repeat(antCount) {
                val route = buildRoute(
                    nodeCount = landmarks.size,
                    pheromone = pheromone,
                    distances = matrix.distances,
                    alpha = request.config.alpha,
                    beta = request.config.beta,
                    random = random
                )
                val score = routeDistance(route, matrix.distances)
                iterationRoutes.add(route to score)

                if (score < bestScore) {
                    bestScore = score
                    bestRoute = route
                }
            }

            evaporatePheromone(pheromone, request.config.evaporation)
            iterationRoutes.forEach { (route, score) ->
                depositRoutePheromone(pheromone, route, score, request.config.q)
            }

            val bestOrderPois = bestRoute.map { landmarks[it - 1] }
            val bestPath = buildRouteCells(bestRoute, matrix.paths, request.startCell)
            emit(
                AntTourProgress(
                    iteration = iterIndex + 1,
                    totalIterations = request.config.iterations,
                    bestScore = bestScore,
                    bestVisitOrder = bestOrderPois,
                    bestPath = bestPath
                )
            )
        }
    }

    fun runCoworkOptimization(request: AntCoworkRequest): Flow<AntCoworkProgress> = flow {
        val spaces = request.spaces
            .asSequence()
            .filter { it.typeId == "student_space" && it.spaceBonus != null }
            .distinctBy { it.id }
            .toList()

        if (spaces.isEmpty() || request.studentCount <= 0) {
            emit(
                AntCoworkProgress(
                    iteration = 0,
                    totalIterations = request.config.iterations,
                    bestScore = 0.0,
                    primarySpace = null,
                    allocation = emptyList(),
                    primaryPath = listOf(request.startCell)
                )
            )
            return@flow
        }

        val random = request.config.randomSeed?.let { Random(it) } ?: Random.Default
        val startPaths = spaces.map {
            val path = astar(request.grid, request.startCell, it.row to it.col)
            path ?: emptyList()
        }
        val distances = startPaths.map { if (it.isEmpty()) request.config.unreachablePenalty else (it.size - 1).toDouble() }

        val pheromone = DoubleArray(spaces.size) { 1.0 }

        var bestScore = Double.MAX_VALUE
        var bestLoad = IntArray(spaces.size)

        emit(
            AntCoworkProgress(
                iteration = 0,
                totalIterations = request.config.iterations,
                bestScore = bestScore,
                primarySpace = null,
                allocation = emptyList(),
                primaryPath = listOf(request.startCell)
            )
        )

        repeat(request.config.iterations) { iterIndex ->
            val load = IntArray(spaces.size)

            repeat(request.studentCount) {
                val chosen = chooseSpaceForStudent(
                    spaces = spaces,
                    currentLoad = load,
                    distances = distances,
                    pheromone = pheromone,
                    alpha = request.config.alpha,
                    beta = request.config.beta,
                    random = random,
                    unreachablePenalty = request.config.unreachablePenalty
                )
                load[chosen] += 1
            }

            val score = evaluateCoworkLoad(
                spaces = spaces,
                load = load,
                distances = distances,
                comfortWeight = request.config.comfortWeight,
                overloadPenalty = request.config.overloadPenalty,
                unreachablePenalty = request.config.unreachablePenalty
            )

            if (score < bestScore) {
                bestScore = score
                bestLoad = load.copyOf()
            }

            evaporate(pheromone, request.config.evaporation)
            deposit(pheromone, load, score, request.config.q)

            val primaryIndex = bestLoad.indices.maxByOrNull { idx ->
                val assigned = bestLoad[idx]
                val bonus = spaces[idx].spaceBonus?.comfort ?: 0f
                assigned * 10 + bonus
            }
            val primary = primaryIndex?.let { spaces[it] }
            val primaryPath = primaryIndex?.let { idx -> startPaths[idx] }?.takeIf { it.isNotEmpty() }
                ?: listOf(request.startCell)

            val allocations = bestLoad.indices
                .mapNotNull { idx ->
                    val assigned = bestLoad[idx]
                    if (assigned <= 0) return@mapNotNull null
                    val bonus = spaces[idx].spaceBonus
                    AntCoworkAllocation(
                        poi = spaces[idx],
                        assignedStudents = assigned,
                        capacity = bonus?.capacity ?: 0,
                        comfort = bonus?.comfort ?: 0f
                    )
                }
                .sortedByDescending { it.assignedStudents }

            emit(
                AntCoworkProgress(
                    iteration = iterIndex + 1,
                    totalIterations = request.config.iterations,
                    bestScore = bestScore,
                    primarySpace = primary,
                    allocation = allocations,
                    primaryPath = primaryPath
                )
            )
        }
    }

    private fun buildDistanceMatrix(
        grid: Array<IntArray>,
        start: Pair<Int, Int>,
        landmarks: List<PoiItem>,
        unreachablePenalty: Double
    ): DistanceMatrix {
        val nodes = mutableListOf(start)
        nodes.addAll(landmarks.map { it.row to it.col })

        val n = nodes.size
        val distances = Array(n) { DoubleArray(n) { 0.0 } }
        val paths = mutableMapOf<Pair<Int, Int>, List<Pair<Int, Int>>>()

        for (i in 0 until n) {
            for (j in 0 until n) {
                if (i == j) {
                    distances[i][j] = 0.0
                    paths[i to j] = listOf(nodes[i])
                    continue
                }

                if (paths.containsKey(j to i)) {
                    val mirrored = paths[j to i].orEmpty().asReversed()
                    paths[i to j] = mirrored
                    distances[i][j] = distances[j][i]
                    continue
                }

                val path = astar(grid, nodes[i], nodes[j])
                if (path == null || path.isEmpty()) {
                    distances[i][j] = unreachablePenalty
                    paths[i to j] = emptyList()
                } else {
                    distances[i][j] = (path.size - 1).toDouble()
                    paths[i to j] = path
                }
            }
        }

        return DistanceMatrix(distances, paths)
    }

    private fun buildRoute(
        nodeCount: Int,
        pheromone: Array<DoubleArray>,
        distances: Array<DoubleArray>,
        alpha: Double,
        beta: Double,
        random: Random
    ): List<Int> {
        val visited = BooleanArray(nodeCount + 1)
        visited[0] = true

        var current = 0
        val route = mutableListOf<Int>()

        while (route.size < nodeCount) {
            val candidates = (1..nodeCount).filter { !visited[it] }
            val next = rouletteSelectNode(current, candidates, pheromone, distances, alpha, beta, random)
                ?: candidates.minByOrNull { distances[current][it] } ?: candidates.first()

            route.add(next)
            visited[next] = true
            current = next
        }

        return route
    }

    private fun rouletteSelectNode(
        current: Int,
        candidates: List<Int>,
        pheromone: Array<DoubleArray>,
        distances: Array<DoubleArray>,
        alpha: Double,
        beta: Double,
        random: Random
    ): Int? {
        if (candidates.isEmpty()) return null

        val weights = candidates.map { candidate ->
            val tau = pheromone[current][candidate].pow(alpha)
            val eta = (1.0 / (distances[current][candidate] + 1e-9)).pow(beta)
            tau * eta
        }
        val sum = weights.sum()
        if (sum <= 0.0) return null

        val point = random.nextDouble() * sum
        var cursor = 0.0
        for (i in candidates.indices) {
            cursor += weights[i]
            if (point <= cursor) return candidates[i]
        }
        return candidates.last()
    }

    private fun routeDistance(route: List<Int>, distances: Array<DoubleArray>): Double {
        var total = 0.0
        var current = 0
        for (next in route) {
            total += distances[current][next]
            current = next
        }
        return total
    }

    private fun buildRouteCells(
        route: List<Int>,
        paths: Map<Pair<Int, Int>, List<Pair<Int, Int>>>,
        start: Pair<Int, Int>
    ): List<Pair<Int, Int>> {
        if (route.isEmpty()) return listOf(start)

        val result = mutableListOf<Pair<Int, Int>>()
        var current = 0
        route.forEach { next ->
            val segment = paths[current to next].orEmpty()
            if (segment.isNotEmpty()) {
                if (result.isEmpty()) result.addAll(segment) else result.addAll(segment.drop(1))
            }
            current = next
        }
        return if (result.isEmpty()) listOf(start) else result
    }

    private fun evaporatePheromone(pheromone: Array<DoubleArray>, evaporation: Double) {
        val factor = (1.0 - evaporation).coerceIn(0.01, 0.99)
        for (i in pheromone.indices) {
            for (j in pheromone[i].indices) {
                pheromone[i][j] = (pheromone[i][j] * factor).coerceAtLeast(0.0001)
            }
        }
    }

    private fun depositRoutePheromone(
        pheromone: Array<DoubleArray>,
        route: List<Int>,
        score: Double,
        q: Double
    ) {
        if (route.isEmpty() || score <= 0.0) return
        val delta = q / score

        var current = 0
        route.forEach { next ->
            pheromone[current][next] += delta
            pheromone[next][current] += delta
            current = next
        }
    }

    private fun chooseSpaceForStudent(
        spaces: List<PoiItem>,
        currentLoad: IntArray,
        distances: List<Double>,
        pheromone: DoubleArray,
        alpha: Double,
        beta: Double,
        random: Random,
        unreachablePenalty: Double
    ): Int {
        val weights = spaces.indices.map { idx ->
            val distance = distances[idx]
            if (distance >= unreachablePenalty) {
                0.0
            } else {
                val capacity = (spaces[idx].spaceBonus?.capacity ?: 0).coerceAtLeast(1)
                val comfort = (spaces[idx].spaceBonus?.comfort ?: 0f).toDouble().coerceAtLeast(0.0)
                val remainingFactor = ((capacity - currentLoad[idx]).coerceAtLeast(0) + 1).toDouble() / (capacity + 1)
                val attractiveness = ((comfort + 1.0) * remainingFactor) / (distance + 1.0)
                pheromone[idx].pow(alpha) * attractiveness.pow(beta)
            }
        }

        val total = weights.sum()
        if (total <= 0.0) {
            return spaces.indices.minByOrNull { distances[it] } ?: 0
        }

        val point = random.nextDouble() * total
        var cursor = 0.0
        for (i in weights.indices) {
            cursor += weights[i]
            if (point <= cursor) return i
        }

        return weights.lastIndex
    }

    private fun evaluateCoworkLoad(
        spaces: List<PoiItem>,
        load: IntArray,
        distances: List<Double>,
        comfortWeight: Double,
        overloadPenalty: Double,
        unreachablePenalty: Double
    ): Double {
        var score = 0.0

        for (idx in spaces.indices) {
            val students = load[idx]
            if (students <= 0) continue

            val distance = distances[idx]
            if (distance >= unreachablePenalty) {
                score += unreachablePenalty * students
                continue
            }

            val capacity = (spaces[idx].spaceBonus?.capacity ?: 0).coerceAtLeast(1)
            val comfort = (spaces[idx].spaceBonus?.comfort ?: 0f).toDouble()
            val overload = (students - capacity).coerceAtLeast(0)

            score += distance * students
            score -= comfort * comfortWeight * students
            score += overload.toDouble() * overload.toDouble() * overloadPenalty
        }

        return score
    }

    private fun evaporate(pheromone: DoubleArray, evaporation: Double) {
        val factor = (1.0 - evaporation).coerceIn(0.01, 0.99)
        for (i in pheromone.indices) {
            pheromone[i] = (pheromone[i] * factor).coerceAtLeast(0.0001)
        }
    }

    private fun deposit(pheromone: DoubleArray, load: IntArray, score: Double, q: Double) {
        if (score <= 0.0) return
        val deltaBase = q / score
        for (i in pheromone.indices) {
            if (load[i] > 0) {
                pheromone[i] += deltaBase * load[i]
            }
        }
    }
}

private data class DistanceMatrix(
    val distances: Array<DoubleArray>,
    val paths: Map<Pair<Int, Int>, List<Pair<Int, Int>>>
)
