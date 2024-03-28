package algorithm.crossover

import genome.NetworkGenome
import kotlin.random.Random
fun <T> selectGene(group: List<T>, parent1Genes: List<T>, parent2Genes: List<T>, random: Random, biasTowardsParent1: Double): T {
    return if (group.size == 1) group.first()
    else {
        // println("Selecting gene from group of size ${group.size}")
        if (random.nextDouble() < biasTowardsParent1) group.first { it in parent1Genes }
        else group.first { it in parent2Genes }
    }
}

fun biasedCrossover(
    parent1: NetworkGenome,
    parent2: NetworkGenome,
    random: Random,
    biasTowardsParent1: Double = 0.75
): NetworkGenome {
    val childNodes = (parent1.nodeGenomes + parent2.nodeGenomes)
        .groupBy { it.id }
        .values
        .map { group -> selectGene(group, parent1.nodeGenomes, parent2.nodeGenomes, random, biasTowardsParent1) }

    val childConnectionGenes = (parent1.connectionGenes + parent2.connectionGenes)
        .groupBy { it.id }
        .values
        .map { group ->

            try {
                selectGene(group, parent1.connectionGenes, parent2.connectionGenes, random, biasTowardsParent1)
            } catch (e: Exception) {
                println("")
                group.forEach {
                    println(it)
                }
                val parent1 = parent1.connectionGenes
                val parent2 = parent2.connectionGenes
                println("is c1 from parent 1 = ${group.first() in parent1}")
                println("is c1 from parent 2 = ${group.first() in parent2}")
                println("is c2 from parent 1 = ${group[1] in parent1}")
                println("is c2 from parent 2 = ${group[1] in parent2}")

                throw RuntimeException("Caught exception $e")
            }
        }

    return NetworkGenome(childNodes, childConnectionGenes,
        fitness = null,
        sharedFitness = null,
        speciesId = null)
}

class BiasedCrossover(val random: Random, val biasTowardsParent1: Double = 0.75) : CrossMutation {
    override fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome {
        return biasedCrossover(parent1, parent2, random, biasTowardsParent1)
    }
}