package algorithm

import algorithm.operator.GeneticOperator
import genome.NetworkGenome
import kotlin.random.Random

interface GenomeMutator {
    fun mutateGenome(genome: NetworkGenome): NetworkGenome
}

data class MutationOperation(val probability: Double, val operation: GeneticOperator)

fun createMutationOperations(geneticOperators: GeneticOperators): List<MutationOperation> {
    return listOf(
            MutationOperation(0.04, geneticOperators.mutateAddConnection),
            MutationOperation(0.01, geneticOperators.mutateAddNode),
            MutationOperation(0.9, geneticOperators.mutateWeights),
            MutationOperation(0.04, geneticOperators.mutateActivationFunction),
            MutationOperation(0.05, geneticOperators.mutateConnectionEnabled)
    )
}

class DefaultGenomeMutator(val mutationOperations: List<MutationOperation>, val random: Random) :
        GenomeMutator {

    override fun mutateGenome(genome: NetworkGenome): NetworkGenome {
        val mutatedGenome =
                mutationOperations.fold(genome) { currentGenome, mutationOperation ->
                    if (random.nextDouble() < mutationOperation.probability) {
                        mutationOperation.operation.apply(currentGenome)
                    } else {
                        currentGenome
                    }
                }
        return mutatedGenome
    }
}
