package algorithm

import genome.NetworkGenome

interface FitnessEvaluator {
    fun calculateFitness(genome: NetworkGenome): Double
}