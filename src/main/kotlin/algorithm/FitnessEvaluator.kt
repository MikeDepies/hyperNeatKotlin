package algorithm

import genome.NetworkGenome
import algorithm.network.NetworkProcessorFactory
import algorithm.network.NetworkBuilder
import algorithm.network.DefaultActivationFunctionMapper
import kotlin.random.Random

interface FitnessEvaluator {
    fun calculateFitness(genome: NetworkGenome): Double
}

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
        // println("Network Details:")
        // println("Number of Nodes: ${genome.nodeGenomes.size}")
        // println("Number of Connections: ${genome.connectionGenes.size}")
        val enabledConnections = genome.connectionGenes.count { it.enabled }
        // println("Number of Enabled Connections: $enabledConnections")
        // println("Fitness: $fitness / ${testCases.size} = ${fitness / testCases.size}")
        return fitness / testCases.size // Average fitness
    }
}

class IrisFitnessEvaluator(private val random : Random) : FitnessEvaluator {
    private val dataset = listOf(
        listOf(5.1, 3.5, 1.4, 0.2) to 0,
        listOf(4.9, 3.0, 1.4, 0.2) to 0,
        listOf(4.7, 3.2, 1.3, 0.2) to 0,
        listOf(5.0, 3.6, 1.4, 0.2) to 0,
        listOf(5.4, 3.9, 1.7, 0.4) to 0,
        listOf(4.6, 3.4, 1.4, 0.3) to 0,
        listOf(5.0, 3.4, 1.5, 0.2) to 0,
        listOf(4.4, 2.9, 1.4, 0.2) to 0,
        listOf(4.9, 3.1, 1.5, 0.1) to 0,
        listOf(5.4, 3.7, 1.5, 0.2) to 0,
        listOf(6.9, 3.1, 4.9, 1.5) to 1,
        listOf(5.5, 2.3, 4.0, 1.3) to 1,
        listOf(6.5, 2.8, 4.6, 1.5) to 1,
        listOf(5.7, 2.8, 4.5, 1.3) to 1,
        listOf(6.3, 3.3, 4.7, 1.6) to 1,
        listOf(4.9, 2.4, 3.3, 1.0) to 1,
        listOf(6.6, 2.9, 4.6, 1.3) to 1,
        listOf(5.2, 2.7, 3.9, 1.4) to 1,
        listOf(5.0, 2.0, 3.5, 1.0) to 1,
        listOf(5.9, 3.0, 4.2, 1.5) to 1,
        listOf(6.0, 3.4, 4.5, 1.6) to 1,
        listOf(6.1, 2.6, 5.6, 1.4) to 2,
        listOf(5.8, 2.6, 4.0, 1.2) to 2,
        listOf(7.1, 3.0, 5.9, 2.1) to 2,
        listOf(6.3, 2.9, 5.6, 1.8) to 2,
        listOf(6.5, 3.0, 5.8, 2.2) to 2,
        listOf(7.6, 3.0, 6.6, 2.1) to 2,
        listOf(4.9, 2.5, 4.5, 1.7) to 2,
        listOf(7.3, 2.9, 6.3, 1.8) to 2,
        listOf(6.7, 2.5, 5.8, 1.8) to 2,
        listOf(7.2, 3.6, 6.1, 2.5) to 2,
        listOf(6.5, 3.2, 5.1, 2.0) to 2,
        listOf(6.4, 2.7, 5.3, 1.9) to 2,
        listOf(6.8, 3.0, 5.5, 2.1) to 2,
        listOf(5.7, 2.5, 5.0, 2.0) to 2,
        listOf(5.8, 2.8, 5.1, 2.4) to 2,
        listOf(6.4, 3.2, 5.3, 2.3) to 2,
        listOf(6.5, 3.0, 5.5, 1.8) to 2,
        listOf(7.7, 3.8, 6.7, 2.2) to 2,
        listOf(7.7, 2.6, 6.9, 2.3) to 2,
        listOf(6.0, 2.2, 5.0, 1.5) to 2,
        listOf(6.9, 3.2, 5.7, 2.3) to 2,
        listOf(5.6, 2.8, 4.9, 2.0) to 2,
        listOf(7.7, 2.8, 6.7, 2.0) to 2,
        listOf(6.3, 2.7, 4.9, 1.8) to 2,
        listOf(6.7, 3.3, 5.7, 2.1) to 2,
        listOf(7.2, 3.2, 6.0, 1.8) to 2,
        listOf(6.2, 2.8, 4.8, 1.8) to 2,
        listOf(6.1, 3.0, 4.9, 1.8) to 2,
        listOf(6.4, 2.8, 5.6, 2.1) to 2,
        listOf(7.2, 3.0, 5.8, 1.6) to 2,
        listOf(7.4, 2.8, 6.1, 1.9) to 2,
        listOf(7.9, 3.8, 6.4, 2.0) to 2,
        listOf(6.4, 2.8, 5.6, 2.2) to 2,
        listOf(6.3, 2.8, 5.1, 1.5) to 2,
        listOf(6.1, 2.6, 5.6, 1.4) to 2,
        listOf(7.7, 3.0, 6.1, 2.3) to 2,
        listOf(6.3, 3.4, 5.6, 2.4) to 2,
        listOf(6.4, 3.1, 5.5, 1.8) to 2,
        listOf(6.0, 3.0, 4.8, 1.8) to 2,
        listOf(6.9, 3.1, 5.4, 2.1) to 2,
        listOf(6.7, 3.1, 5.6, 2.4) to 2,
        listOf(6.9, 3.1, 5.1, 2.3) to 2,
        listOf(5.8, 2.7, 5.1, 1.9) to 2,
        listOf(6.8, 3.2, 5.9, 2.3) to 2,
        listOf(6.7, 3.3, 5.7, 2.5) to 2,
        listOf(6.7, 3.0, 5.2, 2.3) to 2,
        listOf(6.3, 2.5, 5.0, 1.9) to 2,
        listOf(6.5, 3.0, 5.2, 2.0) to 2,
        listOf(6.2, 3.4, 5.4, 2.3) to 2,
        listOf(5.9, 3.0, 5.1, 1.8) to 2
    )

    override fun calculateFitness(genome: NetworkGenome): Double {
        val networkProcessorFactory = NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()))
        val networkProcessor = networkProcessorFactory.createProcessor(genome)
        var correctClassifications = 0

        for ((inputs, expectedClass) in dataset.shuffled(random)) {
            val output = networkProcessor.feedforward(inputs)
            val predictedClass = output.indexOf(output.maxOrNull() ?: 0.0)
            if (predictedClass == expectedClass) correctClassifications++
        }

        return correctClassifications.toDouble() / dataset.size
    }
}
