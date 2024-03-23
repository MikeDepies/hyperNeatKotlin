package ai.sunnystratgies

data class NodeGenome(val id: Int, val type: NodeType, var activationFunction: ActivationFunction, val bias: Double)
data class ConnectionGenome(val id: Int, val inputNode: NodeGenome, val outputNode: NodeGenome, val weight: Double, val enabled: Boolean)
enum class NodeType {
    INPUT,
    HIDDEN,
    OUTPUT
}

enum class ActivationFunction {
    SIGMOID,
    TANH,
    RELU
}
data class NetworkGenome(val nodeGenomes : List<NodeGenome>, val ConnectionGenomes : List<ConnectionGenome>)
interface GenomeMutator {
    fun mutateGenome(genome: NetworkGenome)

}
data class Species(
    val id: Int,
    val members: MutableList<NetworkGenome>,
    var representative: NetworkGenome,
    var averageFitness: Double,
    var staleness: Int = 0
)

data class Population(
    val genomes: MutableList<NetworkGenome>,
    val species: MutableList<Species>,
    var generation: Int = 0
)

object InnovationTracker {
    private var currentInnovationNumber = 0
    fun getNextInnovationNumber(): Int = currentInnovationNumber++
}

interface GeneticOperators {
    fun crossover(parent1: NetworkGenome, parent2: NetworkGenome): NetworkGenome
    fun mutateAddNode(genome: NetworkGenome)
    fun mutateAddConnection(genome: NetworkGenome)
    fun mutateWeights(genome: NetworkGenome)
}

interface FitnessEvaluator {
    fun calculateFitness(genome: NetworkGenome): Double
}
//this class is responsible for implementing the NEAT algorithm
class NEAT {
    // Initialize population
    fun initializePopulation() {
        // TODO: Implement population initialization
    }

    // Evaluate genomes
    fun evaluateGenomes() {
        // TODO: Implement genome evaluation
    }

    // Select genomes for reproduction
    fun selectGenomes() {
        // TODO: Implement genome selection
    }

    // Reproduce selected genomes
    fun reproduceGenomes() {
        // TODO: Implement genome reproduction
    }

    // Mutate genomes
    fun mutateGenomes() {
        // TODO: Implement genome mutation
    }

    // Add new species
    fun addNewSpecies() {
        // TODO: Implement new species addition
    }

    // Remove stale species
    fun removeStaleSpecies() {
        // TODO: Implement stale species removal
    }

    // Share fitness within species
    fun shareFitness() {
        // TODO: Implement fitness sharing
    }

    // Adjust compatibility threshold
    fun adjustCompatibilityThreshold() {
        // TODO: Implement compatibility threshold adjustment
    }

}

