package algorithm

import algorithm.operator.GeneticOperator
import genome.NetworkGenome
import kotlin.random.Random
import util.GenomeTestUtils

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
                        currentGenome
                    }
                }
                
        return mutatedGenome
    }
}
