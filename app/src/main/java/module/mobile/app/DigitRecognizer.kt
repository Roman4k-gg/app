package module.mobile.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DigitRecognizer(private val context: Context) {
    private val inputSize = 50 * 50
    private val hiddenSizes = listOf(128, 128)
    private val outputSize = 10
    private val network = NeuralNetwork(inputSize, hiddenSizes, outputSize)

    suspend fun loadModel() = withContext(Dispatchers.IO) {
        try {
            val weights = context.assets.open("weights.txt").bufferedReader().useLines { lines ->
                lines.flatMap { line -> line.trim().split(Regex("\\s+")).map { it.toDouble() } }.toList()
            }
            network.loadWeights(weights)
        } catch (e: Exception) {
            e.printStackTrace()
            // Если файла нет, работаем со случайными весами (результат будет случайным)
        }
    }

    fun recognize(imagePixels: FloatArray): Int {
        require(imagePixels.size == inputSize)
        val inputMatrix = Matrix.fromList(imagePixels.map { it.toDouble() }, 1, inputSize)
        return network.predict(inputMatrix)
    }
}