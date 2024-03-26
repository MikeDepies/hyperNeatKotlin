package algorithm

import genome.NetworkGenome
import algorithm.network.NetworkProcessorFactory
import algorithm.network.NetworkBuilder
import algorithm.network.DefaultActivationFunctionMapper
import environment.*
import kotlin.random.Random

interface FitnessEvaluator {
    fun calculateFitness(genome: NetworkGenome): Double
}
