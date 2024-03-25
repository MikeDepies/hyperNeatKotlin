package algorithm.crossover

import genome.NetworkGenome
import kotlin.random.Random

class RandomCrossover(val random: Random) : CrossMutation {
    override fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome {
        return randomCrossover(parent1, parent2, random)
    }
}

fun randomCrossover(parent1: NetworkGenome, parent2: NetworkGenome, random: Random): NetworkGenome {
    // Assuming both parents have the same set of node IDs but possibly with different attributes
    val childNodes =
            (parent1.nodeGenomes + parent2.nodeGenomes).groupBy { it.id }.values.map {
                if (it.size == 1) it.first() else it.random(random)
            }

    val childConnectionGenes =
            (parent1.connectionGenes + parent2.connectionGenes).groupBy { it.id }.values.map {
                if (it.size == 1) it.first() else it.random(random)
            }

    // Construct and return the new child NetworkGenome
    return NetworkGenome(childNodes, childConnectionGenes)
}