package algorithm.fitnessevaluator

import kotlin.random.Random
import algorithm.FitnessEvaluator
import algorithm.network.DefaultActivationFunctionMapper
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import genome.NetworkGenome

class IrisFitnessEvaluator(private val random : Random) : FitnessEvaluator {
    private val dataset = createFakeData()

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
