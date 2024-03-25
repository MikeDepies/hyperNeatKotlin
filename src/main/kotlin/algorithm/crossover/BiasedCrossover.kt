package algorithm.crossover

import genome.NetworkGenome
import kotlin.random.Random

fun biasedCrossover(
    parent1: NetworkGenome,
    parent2: NetworkGenome,
    random: Random,
    biasTowardsParent1: Double = 0.75
): NetworkGenome {
    // Assuming both parents have the same set of node IDs but possibly with different attributes
    val childNodes =
            (parent1.nodeGenomes + parent2.nodeGenomes).groupBy { it.id }.values.map { group ->
                if (group.size == 1) group.first()
                else {
                    // Bias towards parent1
                    if (random.nextDouble() < biasTowardsParent1)
                            group.first { it in parent1.nodeGenomes }
                    else group.first { it in parent2.nodeGenomes }
                }
            }

    val childConnectionGenes =
            (parent1.connectionGenes + parent2.connectionGenes).groupBy { it.id }.values.map { group
                ->
                if (group.size == 1) group.first()
                else {
                    // Bias towards parent1
                    if (random.nextDouble() < biasTowardsParent1)
                            group.first { it in parent1.connectionGenes }
                    else group.first { it in parent2.connectionGenes }
                }
            }

    // Construct and return the new child NetworkGenome
    return NetworkGenome(childNodes, childConnectionGenes)
}

class BiasedCrossover(val random: Random, val biasTowardsParent1: Double = 0.75) : CrossMutation {
    override fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome {
        return biasedCrossover(parent1, parent2, random, biasTowardsParent1)
    }
}