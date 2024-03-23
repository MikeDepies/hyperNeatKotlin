package com.github.mikedepies

interface GeneticOperators {
    fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome
    fun mutateAddNode(genome: NetworkGenome)
    fun mutateAddConnection(genome: NetworkGenome)
    fun mutateWeights(genome: NetworkGenome)
}