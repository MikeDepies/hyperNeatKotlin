package algorithm.fitnessevaluator

import algorithm.FitnessEvaluator
import genome.NetworkGenome
import algorithm.network.NetworkProcessorFactory
import algorithm.network.NetworkBuilder
import algorithm.network.DefaultActivationFunctionMapper
import environment.*
import kotlin.random.Random

class XorFitnessEvaluator : FitnessEvaluator {
    private val testCases = listOf(
        listOf(0.0, 0.0) to 0.0,
        listOf(0.0, 1.0) to 1.0,
        listOf(1.0, 0.0) to 1.0,
        listOf(1.0, 1.0) to 0.0
    )

    override fun calculateFitness(genome: NetworkGenome): Double {
        val networkProcessorFactory = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()))
        val networkProcessor = networkProcessorFactory.createProcessor(genome)
        var fitness = 0.0

        for ((inputs, expectedOutput) in testCases) {
            val output = networkProcessor.feedforward(inputs).first()
            val error = Math.abs(output - expectedOutput)
            fitness += 1.0 - error // Higher fitness for lower error
        }
        return fitness / testCases.size // Average fitness
    }
}
