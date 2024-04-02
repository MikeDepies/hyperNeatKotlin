package algorithm

import algorithm.crossover.CrossMutation
import algorithm.operator.GeneticOperator
import genome.NetworkGenome
import kotlin.random.Random
import util.GenomeTestUtils

interface GenomeMutator {
    fun mutateGenome(genome: NetworkGenome): NetworkGenome
    fun crossMutateGenomes(genome1: NetworkGenome, genome2: NetworkGenome): NetworkGenome
    fun rollCrossMutation(): Boolean
}

data class MutationOperation(val probability: Double, val operation: GeneticOperator)
data class CrossOverOperation(val operation: CrossMutation, val probability: Double = .7)
data class GenomeMutatorConfig(
    val mutationOperations: List<MutationOperation>,
    val crossOverOperations: CrossOverOperation,
    val random: Random
)

fun createMutationOperations(geneticOperators: GeneticOperators, random: Random): GenomeMutatorConfig {
    return GenomeMutatorConfig(
        listOf(
            MutationOperation(0.04, geneticOperators.mutateAddConnection),
            MutationOperation(0.01, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.04, geneticOperators.mutateActivationFunction),
            MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
        ), CrossOverOperation(geneticOperators.crossMutation), random
    )
}

fun fromList(operations: List<MutationOperation>, crossOverOperations: CrossOverOperation, random: Random): GenomeMutatorConfig {
    return GenomeMutatorConfig(operations, crossOverOperations, random)
}

class DefaultGenomeMutator(val config: GenomeMutatorConfig) :
    GenomeMutator {

    override fun mutateGenome(genome: NetworkGenome): NetworkGenome {
        val mutatedGenome =
            config.mutationOperations.fold(genome) { currentGenome, mutationOperation ->
                if (config.random.nextDouble() < mutationOperation.probability) {
                    val mutatedGenome = mutationOperation.operation.apply(currentGenome)
                    if (GenomeTestUtils.countGenomesWithOverlappingConnectionIds(listOf(mutatedGenome)) > 0) {

                        println(mutationOperation)
                        println(GenomeTestUtils.generateConflictedConnectionIds(listOf(mutatedGenome)))
                        mutatedGenome.connectionGenes.forEach { connectionGene ->

                            println(connectionGene)

                        }
                        throw RuntimeException("Mutated genome has overlapping connection ids")
                    }
                    mutatedGenome
                } else {
                    currentGenome.copy(
                        fitness = null,
                        sharedFitness = null,
                        speciesId = null
                    )
                }
            }

        return mutatedGenome
    }

    override fun crossMutateGenomes(genome1: NetworkGenome, genome2: NetworkGenome): NetworkGenome {
        return config.crossOverOperations.operation.crossover(genome1, genome2)
    }

    override fun rollCrossMutation(): Boolean {
        return config.random.nextDouble() < config.crossOverOperations.probability
    }
}
