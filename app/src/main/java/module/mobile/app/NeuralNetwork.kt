package module.mobile.app

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
        require(cols == other.rows)
        return Matrix(rows, other.cols) { i, j ->
            (0 until cols).sumOf { k -> data[i][k] * other.data[k][j] }
        }
    }
    fun transpose() = Matrix(cols, rows) { i, j -> data[j][i] }

    companion object {
        fun fromList(list: List<Double>, rows: Int, cols: Int) = Matrix(rows, cols) { i, j ->
            list[i * cols + j]
        }
    }
}

object Activations {
    fun relu(x: Double) = if (x > 0) x else 0.0
    fun reluDerivative(x: Double) = if (x > 0) 1.0 else 0.0
    fun softmax(m: Matrix): Matrix {
        val expM = m.map { exp(it) }
        val sum = expM.data.sumOf { row -> row.sum() }
        return expM.map { it / sum }
    }
}

object Loss {
    fun crossEntropy(pred: Matrix, target: Matrix): Double {
        var loss = 0.0
        for (i in 0 until pred.rows)
            for (j in 0 until pred.cols)
                loss -= target.data[i][j] * kotlin.math.ln(pred.data[i][j] + 1e-8)
        return loss / pred.rows
    }
}

class DenseLayer(val inputSize: Int, val outputSize: Int) {
    val weights = Matrix(inputSize, outputSize)
    val biases = Matrix(1, outputSize)
    var input: Matrix? = null
    var gradWeights: Matrix? = null

    init {
        val std = sqrt(2.0 / inputSize)
        weights.randomize(-std, std)
        biases.randomize(-std, std)
    }

    private fun Matrix.randomize(min: Double, max: Double) {
        for (i in 0 until rows)
            for (j in 0 until cols)
                data[i][j] = Random.nextDouble(min, max)
    }

    fun forward(input: Matrix): Matrix {
        this.input = input
        return input.matMul(weights).add(biases)
    }

    fun backward(gradOutput: Matrix, learningRate: Double): Matrix {
        val gradWeights = input!!.transpose().matMul(gradOutput)
        val gradBiases = Matrix(1, outputSize) { _, j -> gradOutput.data.sumOf { it[j] } }
        for (i in 0 until weights.rows)
            for (j in 0 until weights.cols)
                weights.data[i][j] -= learningRate * gradWeights.data[i][j]
        for (j in 0 until biases.cols)
            biases.data[0][j] -= learningRate * gradBiases.data[0][j]

        return gradOutput.matMul(weights.transpose())
    }
}

class NeuralNetwork(private val inputSize: Int, private val hiddenSizes: List<Int>, private val outputSize: Int) {
    val layers = mutableListOf<DenseLayer>()
    private var hiddenOutputs = mutableListOf<Matrix>()

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
        hiddenOutputs.clear()
        for (i in layers.indices) {
            current = layers[i].forward(current)
            if (i < layers.size - 1) {
                current = current.map(Activations::relu)
                hiddenOutputs.add(current)
            } else {
                current = Activations.softmax(current)
            }
        }
        return current
    }

    fun predict(input: Matrix): Int {
        val output = forward(input)
        return output.data[0].indices.maxByOrNull { output.data[0][it] } ?: -1
    }

    fun loadWeights(flatWeights: List<Double>) {
        var idx = 0
        for (layer in layers) {
            val wSize = layer.weights.rows * layer.weights.cols
            for (i in 0 until layer.weights.rows)
                for (j in 0 until layer.weights.cols)
                    layer.weights.data[i][j] = flatWeights[idx++]
            val bSize = layer.biases.cols
            for (j in 0 until layer.biases.cols)
                layer.biases.data[0][j] = flatWeights[idx++]
        }
    }
}