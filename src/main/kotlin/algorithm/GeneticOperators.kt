package algorithm

import genome.ActivationFunction
import genome.ConnectionGenome
import genome.NetworkGenome
import genome.NodeGenome
import genome.NodeType
import kotlin.random.Random

interface GeneticOperators {
    fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome
    fun mutateAddNode(genome: NetworkGenome): NetworkGenome
    fun mutateAddConnection(genome: NetworkGenome): NetworkGenome
    fun mutateWeights(genome: NetworkGenome): NetworkGenome
}

class GeneticOperatorsImpl(val random: Random, val weightRange: ClosedRange<Double>) :
        GeneticOperators {
    override fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome {
        val childNodeGenomes = parent1.nodeGenomes.union(parent2.nodeGenomes).toList()
        val childConnectionGenes = mutableListOf<ConnectionGenome>()

        parent1.connectionGenes.forEach { parent1Gene ->
            val matchingGene = parent2.connectionGenes.find { it.id == parent1Gene.id }
            if (matchingGene != null) {
                // If genes match, randomly choose one gene to inherit
                if ((0..1).random() == 0) {
                    childConnectionGenes.add(parent1Gene)
                } else {
                    childConnectionGenes.add(matchingGene)
                }
            } else {
                // If no matching gene, inherit from the more fit parent, assuming parent1 is more
                // fit
                childConnectionGenes.add(parent1Gene)
            }
        }

        // Add non-matching genes from the less fit parent (assuming parent2 is less fit)
        parent2.connectionGenes
                .filter { parent2Gene -> parent1.connectionGenes.none { it.id == parent2Gene.id } }
                .forEach { nonMatchingGene -> childConnectionGenes.add(nonMatchingGene) }

        return NetworkGenome(childNodeGenomes, childConnectionGenes)
    }

    override fun mutateAddNode(genome: NetworkGenome): NetworkGenome {
        val randomConnectionIndex = random.nextInt(genome.connectionGenes.size)
        val connectionToSplit = genome.connectionGenes[randomConnectionIndex]

        // Disable the old connection
        val disabledConnection = connectionToSplit.copy(enabled = false)

        // Create a new node
        val newNodeId = genome.nodeGenomes.maxOf { it.id } + 1
        val newNode = NodeGenome(newNodeId, NodeType.HIDDEN, ActivationFunction.RELU, 0.0)

        // Create two new connections
        val newConnection1 =
                ConnectionGenome(
                        id = genome.connectionGenes.maxOf { it.id } + 1,
                        inputNode = connectionToSplit.inputNode,
                        outputNode = newNode,
                        weight = 1.0,
                        enabled = true
                )
        val newConnection2 =
                ConnectionGenome(
                        id = genome.connectionGenes.maxOf { it.id } + 2,
                        inputNode = newNode,
                        outputNode = connectionToSplit.outputNode,
                        weight = connectionToSplit.weight,
                        enabled = true
                )

        // Update the genome
        val updatedNodeGenomes = genome.nodeGenomes + newNode
        val updatedConnectionGenes =
                genome.connectionGenes.map {
                    if (it.id == connectionToSplit.id) disabledConnection else it
                } + newConnection1 + newConnection2

        return genome.copy(
                nodeGenomes = updatedNodeGenomes,
                connectionGenes = updatedConnectionGenes
        )
    }

    override fun mutateAddConnection(genome: NetworkGenome): NetworkGenome {
        val availableNodes = genome.nodeGenomes
        val possibleConnections =
                availableNodes.flatMap { inputNode ->
                    availableNodes.mapNotNull { outputNode ->
                        if (inputNode != outputNode &&
                                        genome.connectionGenes.none {
                                            it.inputNode.id == inputNode.id &&
                                                    it.outputNode.id == outputNode.id
                                        }
                        ) {
                            Pair(inputNode, outputNode)
                        } else null
                    }
                }

        if (possibleConnections.isNotEmpty()) {
            val (inputNode, outputNode) = possibleConnections.random()
            val newConnectionId = (genome.connectionGenes.maxOfOrNull { it.id } ?: 0) + 1
            val newConnection =
                    ConnectionGenome(
                            id = newConnectionId,
                            inputNode = inputNode,
                            outputNode = outputNode,
                            weight = randomWeight(),
                            enabled = true
                    )

            return genome.copy(connectionGenes = genome.connectionGenes + newConnection)
        }

        // If no possible connections, return the genome as is
        return genome
    }

    override fun mutateWeights(genome: NetworkGenome): NetworkGenome {
        val updatedConnectionGenes = genome.connectionGenes.map { it.copy(weight = randomWeight()) }
        return genome.copy(connectionGenes = updatedConnectionGenes)
    }

    private fun randomWeight(): Double {
        return (random.nextDouble() * (weightRange.endInclusive - weightRange.start) +
                weightRange.start)
    }
}
