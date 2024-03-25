package algorithm.operator

import genome.NetworkGenome

interface GeneticOperator {
    fun apply(genome: NetworkGenome): NetworkGenome
}