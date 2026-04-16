package module.mobile.app.algorithms

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import module.mobile.app.map.model.PoiItem
import kotlin.math.roundToInt
import kotlin.random.Random

private const val MINUTES_IN_DAY = 24 * 60

data class GeneticFoodConfig(
    val populationSize: Int = 80,
    val generations: Int = 120,
    val eliteCount: Int = 4,
    val tournamentSize: Int = 4,
    val crossoverRate: Double = 0.85,
    val mutationRate: Double = 0.20,
    val cellSizeMeters: Double = 1.0,
    val walkSpeedMetersPerSecond: Double = 5_000.0 / 3_600.0,
    val hardPenalty: Double = 1_000_000.0,
    val missingItemPenalty: Double = 8_000.0,
    val closedPoiPenalty: Double = 5_000.0,
    val soonClosingThresholdMinutes: Int = 30,
    val soonClosingBonusFactor: Double = 1.5,
    val randomSeed: Int? = null
)

data class GeneticFoodRequest(
    val grid: Array<IntArray>,
    val startCell: Pair<Int, Int>,
    val cart: Map<String, Int>,
    val pois: List<PoiItem>,
    val startMinuteOfDay: Int,
    val config: GeneticFoodConfig = GeneticFoodConfig()
)

data class GeneticFoodStop(
    val poiId: String,
    val poiName: String,
    val row: Int,
    val col: Int,
    val boughtItems: List<String>,
    val arrivalMinute: Int,
    val leaveMinute: Int
)

data class GeneticFoodCandidate(
    val score: Double,
    val orderedPoiIds: List<String>,
    val orderedPoiNames: List<String>,
    val stops: List<GeneticFoodStop>,
    val fullPath: List<Pair<Int, Int>>,
    val boughtItems: Map<String, Int>,
    val missingItems: Map<String, Int>,
    val travelMinutes: Double
)

data class GeneticFoodProgress(
    val iteration: Int,
    val totalIterations: Int,
    val best: GeneticFoodCandidate
)

class GeneticFoodRouteAlgorithm {

    fun run(request: GeneticFoodRequest): Flow<GeneticFoodProgress> = flow {
        val config = request.config
        val random = config.randomSeed?.let { Random(it) } ?: Random.Default
        val normalizedCart = normalizeCart(request.cart)

        if (normalizedCart.isEmpty()) {
            emit(
                GeneticFoodProgress(
                    iteration = 0,
                    totalIterations = config.generations,
                    best = emptyCandidate(request.startCell)
                )
            )
            return@flow
        }

        val foodNodes = request.pois
            .asSequence()
            .filter { it.typeId == "food" && it.foodBonus != null }
            .mapNotNull { poi ->
                val bonus = poi.foodBonus ?: return@mapNotNull null
                val normalizedMenu = bonus.menu
                    .map { normalizeToken(it) }
                    .filter { it.isNotBlank() }
                    .toSet()
                if (normalizedMenu.none { it in normalizedCart.keys }) return@mapNotNull null
                FoodNode(
                    poi = poi,
                    menu = normalizedMenu,
                    openMinute = parseHmToMinute(bonus.open),
                    closeMinute = parseHmToMinute(bonus.close)
                )
            }
            .toList()

        if (foodNodes.isEmpty()) {
            emit(
                GeneticFoodProgress(
                    iteration = 0,
                    totalIterations = config.generations,
                    best = impossibleCandidate(request.startCell, normalizedCart)
                )
            )
            return@flow
        }

        val distanceCache = buildDistanceCache(
            grid = request.grid,
            start = request.startCell,
            nodes = foodNodes,
            hardPenalty = config.hardPenalty
        )

        val populationSize = config.populationSize.coerceAtLeast(8)
        val population = MutableList(populationSize) { randomPermutation(foodNodes.size, random) }

        var evaluated = population.map {
            evaluateChromosome(
                chromosome = it,
                nodes = foodNodes,
                normalizedCart = normalizedCart,
                startCell = request.startCell,
                startMinuteOfDay = request.startMinuteOfDay,
                distanceCache = distanceCache,
                config = config
            )
        }

        var bestEval = evaluated.minByOrNull { it.score }
            ?: EvaluatedChromosome(
                chromosome = IntArray(0),
                candidate = impossibleCandidate(request.startCell, normalizedCart)
            )
        emit(GeneticFoodProgress(iteration = 0, totalIterations = config.generations, best = bestEval.candidate))

        for (iteration in 1..config.generations) {
            val nextPopulation = mutableListOf<IntArray>()

            evaluated
                .sortedBy { it.score }
                .take(config.eliteCount.coerceAtMost(populationSize))
                .forEach { nextPopulation.add(it.chromosome.copyOf()) }

            while (nextPopulation.size < populationSize) {
                val parentA = tournamentSelect(evaluated, config.tournamentSize, random)
                val parentB = tournamentSelect(evaluated, config.tournamentSize, random)

                val child = if (random.nextDouble() < config.crossoverRate) {
                    orderedCrossover(parentA.chromosome, parentB.chromosome, random)
                } else {
                    parentA.chromosome.copyOf()
                }

                if (random.nextDouble() < config.mutationRate) {
                    mutateSwap(child, random)
                }

                nextPopulation.add(child)
            }

            evaluated = nextPopulation.map {
                evaluateChromosome(
                    chromosome = it,
                    nodes = foodNodes,
                    normalizedCart = normalizedCart,
                    startCell = request.startCell,
                    startMinuteOfDay = request.startMinuteOfDay,
                    distanceCache = distanceCache,
                    config = config
                )
            }

            val iterBest = evaluated.minByOrNull { it.score } ?: bestEval
            if (iterBest.score < bestEval.score) {
                bestEval = iterBest
            }

            emit(GeneticFoodProgress(iteration = iteration, totalIterations = config.generations, best = bestEval.candidate))
        }
    }

    private fun evaluateChromosome(
        chromosome: IntArray,
        nodes: List<FoodNode>,
        normalizedCart: Map<String, Int>,
        startCell: Pair<Int, Int>,
        startMinuteOfDay: Int,
        distanceCache: DistanceCache,
        config: GeneticFoodConfig
    ): EvaluatedChromosome {
        val remaining = normalizedCart.toMutableMap()
        val bought = mutableMapOf<String, Int>()

        var nowMinute = startMinuteOfDay.toDouble()
        var travelMinutes = 0.0
        var penalties = 0.0
        var closingPriorityBonus = 0.0

        val fullPath = mutableListOf(startCell)
        val usedPoiIds = mutableListOf<String>()
        val usedPoiNames = mutableListOf<String>()
        val stops = mutableListOf<GeneticFoodStop>()

        var previous = -1 // -1 = start node

        for (gene in chromosome) {
            if (remaining.isEmpty()) break

            val node = nodes[gene]
            val distance = distanceCache.get(previous, gene)
            previous = gene

            if (distance.isUnreachable) {
                penalties += config.hardPenalty
                continue
            }

            val segmentMinutes = stepsToMinutes(distance.steps, config)
            travelMinutes += segmentMinutes
            nowMinute += segmentMinutes

            appendPathSegment(fullPath, distance.path)

            val neededAtPoi = remaining.keys.filter { it in node.menu }
            if (neededAtPoi.isEmpty()) continue

            val arrivalMinuteRounded = nowMinute.roundToInt()
            val isOpen = isOpenAt(node.openMinute, node.closeMinute, arrivalMinuteRounded)
            if (!isOpen) {
                penalties += config.closedPoiPenalty * neededAtPoi.size
                continue
            }

            val waitMinutes = waitUntilOpen(node.openMinute, node.closeMinute, arrivalMinuteRounded)
            nowMinute += waitMinutes

            val boughtHere = mutableListOf<String>()
            neededAtPoi.forEach { item ->
                val count = remaining[item] ?: 0
                if (count <= 0) return@forEach

                bought[item] = (bought[item] ?: 0) + count
                remaining.remove(item)
                repeat(count) { boughtHere.add(item) }
            }

            if (boughtHere.isNotEmpty()) {
                usedPoiIds.add(node.poi.id)
                usedPoiNames.add(node.poi.name)

                val closeIn = minutesToClose(node.openMinute, node.closeMinute, nowMinute.roundToInt())
                if (closeIn in 0..config.soonClosingThresholdMinutes) {
                    val urgency = (config.soonClosingThresholdMinutes - closeIn + 1).toDouble()
                    closingPriorityBonus += urgency * config.soonClosingBonusFactor
                }

                val minute = positiveDayMinute(nowMinute.roundToInt())
                stops.add(
                    GeneticFoodStop(
                        poiId = node.poi.id,
                        poiName = node.poi.name,
                        row = node.poi.row,
                        col = node.poi.col,
                        boughtItems = boughtHere,
                        arrivalMinute = minute,
                        leaveMinute = minute
                    )
                )
            }
        }

        val missingPenalty = remaining.values.sum() * config.missingItemPenalty
        penalties += missingPenalty

        val score = travelMinutes + penalties - closingPriorityBonus

        val candidate = GeneticFoodCandidate(
            score = score,
            orderedPoiIds = usedPoiIds,
            orderedPoiNames = usedPoiNames,
            stops = stops,
            fullPath = fullPath,
            boughtItems = bought.toMap(),
            missingItems = remaining.toMap(),
            travelMinutes = travelMinutes
        )

        return EvaluatedChromosome(chromosome.copyOf(), candidate)
    }

    private fun buildDistanceCache(
        grid: Array<IntArray>,
        start: Pair<Int, Int>,
        nodes: List<FoodNode>,
        hardPenalty: Double
    ): DistanceCache {
        val cells = mutableListOf(start)
        cells.addAll(nodes.map { Pair(it.poi.row, it.poi.col) })

        val cache = mutableMapOf<Pair<Int, Int>, CachedDistance>()

        for (i in cells.indices) {
            for (j in cells.indices) {
                if (i == j) {
                    cache[i to j] = CachedDistance(steps = 0, path = listOf(cells[i]))
                    continue
                }
                if ((j to i) in cache) {
                    val mirrored = cache[j to i]
                    if (mirrored != null) {
                        cache[i to j] = CachedDistance(
                            steps = mirrored.steps,
                            path = mirrored.path.asReversed()
                        )
                        continue
                    }
                }

                val path = astar(grid, cells[i], cells[j])
                if (path == null || path.isEmpty()) {
                    cache[i to j] = CachedDistance.unreachable(hardPenalty)
                } else {
                    cache[i to j] = CachedDistance(
                        steps = (path.size - 1).coerceAtLeast(0),
                        path = path
                    )
                }
            }
        }

        return DistanceCache(cache)
    }

    private fun randomPermutation(size: Int, random: Random): IntArray {
        val values = IntArray(size) { it }
        for (i in values.lastIndex downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = values[i]
            values[i] = values[j]
            values[j] = tmp
        }
        return values
    }

    private fun tournamentSelect(
        population: List<EvaluatedChromosome>,
        tournamentSize: Int,
        random: Random
    ): EvaluatedChromosome {
        val count = tournamentSize.coerceAtLeast(2)
        var best = population[random.nextInt(population.size)]
        repeat(count - 1) {
            val candidate = population[random.nextInt(population.size)]
            if (candidate.score < best.score) {
                best = candidate
            }
        }
        return best
    }

    // Ordered crossover preserves permutation validity for TSP-like chromosomes.
    private fun orderedCrossover(parentA: IntArray, parentB: IntArray, random: Random): IntArray {
        val size = parentA.size
        if (size < 2) return parentA.copyOf()

        val left = random.nextInt(size)
        val right = random.nextInt(left, size)

        val child = IntArray(size) { -1 }
        val used = BooleanArray(size)

        for (i in left..right) {
            val gene = parentA[i]
            child[i] = gene
            used[gene] = true
        }

        var fillIndex = (right + 1) % size
        var bIndex = (right + 1) % size

        while (child.any { it == -1 }) {
            val gene = parentB[bIndex]
            if (!used[gene]) {
                child[fillIndex] = gene
                used[gene] = true
                fillIndex = (fillIndex + 1) % size
            }
            bIndex = (bIndex + 1) % size
        }

        return child
    }

    private fun mutateSwap(chromosome: IntArray, random: Random) {
        if (chromosome.size < 2) return
        val i = random.nextInt(chromosome.size)
        var j = random.nextInt(chromosome.size)
        while (j == i) {
            j = random.nextInt(chromosome.size)
        }
        val tmp = chromosome[i]
        chromosome[i] = chromosome[j]
        chromosome[j] = tmp
    }

    private fun stepsToMinutes(steps: Int, config: GeneticFoodConfig): Double {
        val meters = steps * config.cellSizeMeters
        val speedMetersPerMinute = config.walkSpeedMetersPerSecond * 60.0
        return if (speedMetersPerMinute <= 0.0) 0.0 else meters / speedMetersPerMinute
    }

    private fun normalizeCart(cart: Map<String, Int>): Map<String, Int> {
        return cart
            .mapNotNull { (item, count) ->
                val token = normalizeToken(item)
                if (token.isBlank() || count <= 0) null else token to count
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, counts) -> counts.sum() }
    }

    private fun emptyCandidate(startCell: Pair<Int, Int>): GeneticFoodCandidate {
        return GeneticFoodCandidate(
            score = 0.0,
            orderedPoiIds = emptyList(),
            orderedPoiNames = emptyList(),
            stops = emptyList(),
            fullPath = listOf(startCell),
            boughtItems = emptyMap(),
            missingItems = emptyMap(),
            travelMinutes = 0.0
        )
    }

    private fun impossibleCandidate(
        startCell: Pair<Int, Int>,
        missing: Map<String, Int>
    ): GeneticFoodCandidate {
        return GeneticFoodCandidate(
            score = Double.MAX_VALUE,
            orderedPoiIds = emptyList(),
            orderedPoiNames = emptyList(),
            stops = emptyList(),
            fullPath = listOf(startCell),
            boughtItems = emptyMap(),
            missingItems = missing,
            travelMinutes = 0.0
        )
    }
}

private data class FoodNode(
    val poi: PoiItem,
    val menu: Set<String>,
    val openMinute: Int,
    val closeMinute: Int
)

private data class CachedDistance(
    val steps: Int,
    val path: List<Pair<Int, Int>>,
    val isUnreachable: Boolean = false,
    val unreachablePenalty: Double = 0.0
) {
    companion object {
        fun unreachable(penalty: Double): CachedDistance {
            return CachedDistance(
                steps = Int.MAX_VALUE,
                path = emptyList(),
                isUnreachable = true,
                unreachablePenalty = penalty
            )
        }
    }
}

private class DistanceCache(
    private val distances: Map<Pair<Int, Int>, CachedDistance>
) {
    fun get(previousGene: Int, currentGene: Int): CachedDistance {
        val from = previousGene + 1 // start index = 0
        val to = currentGene + 1
        return distances[from to to] ?: CachedDistance.unreachable(1_000_000.0)
    }
}

private data class EvaluatedChromosome(
    val chromosome: IntArray,
    val candidate: GeneticFoodCandidate
) {
    val score: Double get() = candidate.score
}

private fun appendPathSegment(target: MutableList<Pair<Int, Int>>, segment: List<Pair<Int, Int>>) {
    if (segment.isEmpty()) return
    val points = if (target.isEmpty()) segment else segment.drop(1)
    target.addAll(points)
}

private fun normalizeToken(value: String): String {
    return value.trim().lowercase()
}

private fun parseHmToMinute(value: String?): Int {
    if (value.isNullOrBlank()) return 0
    val parts = value.split(':')
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: return 0
    val m = parts[1].toIntOrNull() ?: return 0
    return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
}

private fun positiveDayMinute(value: Int): Int {
    val mod = value % MINUTES_IN_DAY
    return if (mod < 0) mod + MINUTES_IN_DAY else mod
}

private fun isOpenAt(openMinute: Int, closeMinute: Int, minuteOfDay: Int): Boolean {
    val t = positiveDayMinute(minuteOfDay)
    if (openMinute == closeMinute) return true
    return if (openMinute < closeMinute) {
        t in openMinute until closeMinute
    } else {
        t >= openMinute || t < closeMinute
    }
}

private fun waitUntilOpen(openMinute: Int, closeMinute: Int, minuteOfDay: Int): Int {
    val t = positiveDayMinute(minuteOfDay)
    if (isOpenAt(openMinute, closeMinute, t)) return 0

    return if (openMinute < closeMinute) {
        if (t < openMinute) {
            openMinute - t
        } else {
            (MINUTES_IN_DAY - t) + openMinute
        }
    } else {
        // Overnight schedule: closed only between [close, open).
        if (t in closeMinute until openMinute) {
            openMinute - t
        } else {
            0
        }
    }
}

private fun minutesToClose(openMinute: Int, closeMinute: Int, minuteOfDay: Int): Int {
    val t = positiveDayMinute(minuteOfDay)
    if (!isOpenAt(openMinute, closeMinute, t)) return 0

    return if (openMinute < closeMinute) {
        closeMinute - t
    } else {
        if (t >= openMinute) {
            (MINUTES_IN_DAY - t) + closeMinute
        } else {
            closeMinute - t
        }
    }
}
