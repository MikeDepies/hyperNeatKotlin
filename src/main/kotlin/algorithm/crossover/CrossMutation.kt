package algorithm.crossover

import genome.NetworkGenome

interface CrossMutation {
    fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome
}