package algorithm.operator

import algorithm.weight.RandomWeight
import genome.NetworkGenome
import kotlin.random.Random
interface MutateWeightsOperator : GeneticOperator {
    val randomWeight: RandomWeight
}
class MutateWeightsOperatorImpl(override val randomWeight: RandomWeight, val random : Random, private val perturbationChance: Double = 0.9, private val perturbationRange: ClosedRange<Double> = -0.1..0.1) : MutateWeightsOperator {
    override fun apply(genome: NetworkGenome): NetworkGenome {
        val updatedConnectionGenes = genome.connectionGenes.map { gene ->
            if (random.nextDouble() < perturbationChance) {
                // Perturb the weight a small amount
                val perturbation = perturbationRange.start + random.nextDouble() * (perturbationRange.endInclusive - perturbationRange.start)
                gene.copy(weight = gene.weight + perturbation)
            } else {
                // Set the weight to a new random value
                gene.copy(weight = randomWeight())
            }
        }
        val updatedNodeGenes = genome.nodeGenomes.map { node ->
            if (random.nextDouble() < perturbationChance) {
                // Perturb the bias a small amount
                val perturbation = perturbationRange.start + random.nextDouble() * (perturbationRange.endInclusive - perturbationRange.start)
                node.copy(bias = node.bias + perturbation)
            } else {
                // Set the bias to a new random value
                node.copy(bias = randomWeight())
            }
        }
        return genome.copy(connectionGenes = updatedConnectionGenes, nodeGenomes = updatedNodeGenes,
            fitness = null,
            sharedFitness = null,
            speciesId = null)
    }
}
