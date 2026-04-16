package module.mobile.app.algorithms

import kotlin.math.exp
import kotlin.math.sqrt
import kotlin.random.Random

class Matrix(val rows: Int, val cols: Int) {
    val data = Array(rows) { DoubleArray(cols) }

    constructor(rows: Int, cols: Int, init: (Int, Int) -> Double) : this(rows, cols) {
        for (i in 0 until rows)
            for (j in 0 until cols) data[i][j] = init(i, j)
    }

    fun copy() = Matrix(rows, cols) { i, j -> data[i][j] }
    fun map(f: (Double) -> Double) = Matrix(rows, cols) { i, j -> f(data[i][j]) }
    fun add(other: Matrix) = Matrix(rows, cols) { i, j -> data[i][j] + other.data[i][j] }
    fun sub(other: Matrix) = Matrix(rows, cols) { i, j -> data[i][j] - other.data[i][j] }
    fun mul(other: Matrix) = Matrix(rows, cols) { i, j -> data[i][j] * other.data[i][j] }

    fun matMul(other: Matrix): Matrix {
        require(cols == other.rows) { "Matrix dimensions mismatch: ${this.rows}x${this.cols} * ${other.rows}x${other.cols}" }
        return Matrix(rows, other.cols) { i, j ->
            var sum = 0.0
            for (k in 0 until cols) {
                sum += data[i][k] * other.data[k][j]
            }
            sum
        }
    }

    fun transpose() = Matrix(cols, rows) { i, j -> data[j][i] }

    companion object {
        fun fromList(list: List<Double>, rows: Int, cols: Int): Matrix {
            require(list.size == rows * cols) { "List size ${list.size} does not match matrix dimensions ${rows}x${cols}" }
            return Matrix(rows, cols) { i, j ->
                list[i * cols + j]
            }
        }
    }
}

object Activations {
    fun relu(x: Double) = if (x > 0) x else 0.0
    fun reluDerivative(x: Double) = if (x > 0) 1.0 else 0.0

    fun softmax(m: Matrix): Matrix {
        val maxPerRow = m.data.map { row -> row.maxOrNull() ?: 0.0 }
        val expM = Matrix(m.rows, m.cols) { i, j ->
            exp(m.data[i][j] - maxPerRow[i])
        }
        val sumPerRow = expM.data.map { row -> row.sum() }
        return Matrix(m.rows, m.cols) { i, j ->
            expM.data[i][j] / (sumPerRow[i] + 1e-8)
        }
    }
}

class DenseLayer(val inputSize: Int, val outputSize: Int) {
    val weights = Matrix(inputSize, outputSize)
    val biases = Matrix(1, outputSize)
    var input: Matrix? = null

    private fun randomizeWeights() {
        val std = sqrt(2.0 / inputSize)
        for (i in 0 until weights.rows)
            for (j in 0 until weights.cols)
                weights.data[i][j] = Random.nextDouble(-std, std)
        for (j in 0 until biases.cols)
            biases.data[0][j] = 0.0
    }

    fun forward(input: Matrix): Matrix {
        this.input = input
        return input.matMul(weights).add(biases)
    }
}

class NeuralNetwork(
    private val inputSize: Int,
    private val hiddenSizes: List<Int>,
    private val outputSize: Int
) {
    private val layers = mutableListOf<DenseLayer>()

    init {
        var prev = inputSize
        for (h in hiddenSizes) {
            layers.add(DenseLayer(prev, h))
            prev = h
        }
        layers.add(DenseLayer(prev, outputSize))
    }

    fun forward(input: Matrix): Matrix {
        var current = input
        for (i in layers.indices) {
            current = layers[i].forward(current)
            if (i < layers.size - 1) {
                current = current.map(Activations::relu)
            } else {
                current = Activations.softmax(current)
            }
        }
        return current
    }

    fun predict(input: Matrix): Int {
        val output = forward(input)
        var maxIndex = 0
        var maxValue = output.data[0][0]
        for (j in 1 until output.cols) {
            if (output.data[0][j] > maxValue) {
                maxValue = output.data[0][j]
                maxIndex = j
            }
        }
        return maxIndex
    }

    fun loadWeights(flatWeights: List<Double>) {
        var idx = 0
        val totalExpected = flatWeights.size

        for (layer in layers) {
            val wRows = layer.weights.rows
            val wCols = layer.weights.cols

            for (i in 0 until wRows) {
                for (j in 0 until wCols) {
                    if (idx < totalExpected) {
                        layer.weights.data[i][j] = flatWeights[idx++]
                    } else {
                        println("Error: Not enough weights in file!")
                        return
                    }
                }
            }
            val bCols = layer.biases.cols
            for (j in 0 until bCols) {
                if (idx < totalExpected) {
                    layer.biases.data[0][j] = flatWeights[idx++]
                } else {
                    println("Error: Not enough biases in file!")
                    return
                }
            }
        }

        if (idx != totalExpected) {
            println("Warning: Loaded $idx weights, but file had $totalExpected. Mismatch!")
        } else {
            println("Successfully loaded $idx weights.")
        }
    }
}