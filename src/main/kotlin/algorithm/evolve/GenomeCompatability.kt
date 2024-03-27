package algorithm.evolve

import genome.NetworkGenome
import population.Species
import kotlin.random.Random

interface GenomeCompatibility {
    fun calculateDistance(genome1: NetworkGenome, genome2: NetworkGenome): Double
}

interface Speciation {
    fun categorizeIntoSpecies(genomes: List<NetworkGenome>)
    fun updateSpeciesRepresentatives()
    val speciesList: List<Species>
}

interface FitnessSharing {
    fun shareFitnessWithinSpecies(species: Species)
}

class FitnessSharingAverage : FitnessSharing {
    override fun shareFitnessWithinSpecies(species: Species) {
        val speciesSize = species.members.size.toDouble()
        for (genome in species.members) {
            genome.sharedFitness = genome.fitness?.div(speciesSize)
        }
    }
}

class FitnessSharingExponential : FitnessSharing {
    override fun shareFitnessWithinSpecies(species: Species) {
        val speciesSize = species.members.size.toDouble()
        val adjustedFitnessSum = species.members.sumOf { Math.exp(it.fitness ?: 0.0) }
        val averageExponentialFitness = adjustedFitnessSum / speciesSize
        for (genome in species.members) {
            genome.sharedFitness = Math.exp(genome.fitness ?: 0.0) / averageExponentialFitness
        }
    }
}

class SpeciationImpl(
        private val compatibilityThreshold: Double,
        private val genomeCompatibility: GenomeCompatibility,
        private val random: Random,
        override val speciesList: MutableList<Species> = mutableListOf(),
) : Speciation {

    override fun categorizeIntoSpecies(genomes: List<NetworkGenome>) {
        speciesList.forEach { it.members.clear() }
        for (genome in genomes) {
            var foundSpecies = false
            for (species in speciesList) {
                val representativeGenome = species.representative
                val distance = genomeCompatibility.calculateDistance(genome, representativeGenome)
                if (distance < compatibilityThreshold) {
                    species.members.add(genome)
                    genome.speciesId = species.id
                    foundSpecies = true
                    break
                }
            }
            if (!foundSpecies) {
                val newSpeciesId = speciesList.size + 1
                // println("Creating new species ($newSpeciesId) for genome ${genome}")
                speciesList.add(Species(newSpeciesId, mutableListOf(genome), genome.copy()))
                genome.speciesId = newSpeciesId
            }
        }
    }
    override fun updateSpeciesRepresentatives() {
        for (species in speciesList) {
            species.representative = species.members.random(random)
        }
    }
}
data class Coefficients(
        val excessCoefficient: Double,
        val disjointCoefficient: Double,
        val weightDiffCoefficient: Double,
)

/* 
    Default coefficients for traditional NEAT
*/
fun createDefaultCoefficients() = Coefficients(1.0, 1.0, 0.4)

class GenomeCompatibilityTraditional(private val coefficients: Coefficients, private val normalizationThreshold: Int = 20) : GenomeCompatibility {

    override fun calculateDistance(genome1: NetworkGenome, genome2: NetworkGenome): Double {
        val excessGenes = calculateExcessGenes(genome1, genome2)
        val disjointGenes = calculateDisjointGenes(genome1, genome2)
        val averageWeightDiff = calculateAverageWeightDiff(genome1, genome2)

        val N = maxOf(genome1.connectionGenes.size, genome2.connectionGenes.size).toDouble()
        val normalizationFactor = if (N < normalizationThreshold) 1.0 else N // Normalizing based on threshold

        return (excessGenes * coefficients.excessCoefficient +
                disjointGenes * coefficients.disjointCoefficient) / normalizationFactor +
                averageWeightDiff * coefficients.weightDiffCoefficient
    }

    private fun calculateExcessGenes(genome1: NetworkGenome, genome2: NetworkGenome): Int {
        val maxInnovation1 = genome1.connectionGenes.maxOfOrNull { it.id } ?: 0
        val maxInnovation2 = genome2.connectionGenes.maxOfOrNull { it.id } ?: 0

        val excessThreshold = minOf(maxInnovation1, maxInnovation2)
        return (genome1.connectionGenes + genome2.connectionGenes).count { it.id > excessThreshold }
    }

    private fun calculateDisjointGenes(genome1: NetworkGenome, genome2: NetworkGenome): Int {
        val maxInnovation1 = genome1.connectionGenes.maxOfOrNull { it.id } ?: 0
        val maxInnovation2 = genome2.connectionGenes.maxOfOrNull { it.id } ?: 0

        val excessThreshold = minOf(maxInnovation1, maxInnovation2)
        return (genome1.connectionGenes + genome2.connectionGenes).count {
            it.id <= excessThreshold &&
                    !genome1.connectionGenes.contains(it) &&
                    !genome2.connectionGenes.contains(it)
        }
    }

    private fun calculateAverageWeightDiff(genome1: NetworkGenome, genome2: NetworkGenome): Double {
        val matchingGenes =
                genome1.connectionGenes.filter { gene1 ->
                    genome2.connectionGenes.any { gene2 -> gene1.id == gene2.id }
                }

        if (matchingGenes.isEmpty()) return 0.0

        val totalWeightDiff =
                matchingGenes.sumOf { gene1 ->
                    val gene2 = genome2.connectionGenes.find { it.id == gene1.id }!!
                    kotlin.math.abs(gene1.weight - gene2.weight)
                }

        return totalWeightDiff / matchingGenes.size
    }
}
