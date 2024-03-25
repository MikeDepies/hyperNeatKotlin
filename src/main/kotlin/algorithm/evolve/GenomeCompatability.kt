package algorithm.evolve

import population.Species
import genome.NetworkGenome


interface GenomeCompatibility {
    fun calculateDistance(genome1: NetworkGenome, genome2: NetworkGenome): Double
}
interface Speciation {
    fun categorizeIntoSpecies(genomes: List<NetworkGenome>)
    fun updateSpeciesRepresentatives()
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
    private val speciesList: MutableList<Species> = mutableListOf()
) : Speciation {
    

    override fun categorizeIntoSpecies(genomes: List<NetworkGenome>) {
        val speciesList = mutableListOf<Species>()

        for (genome in genomes) {
            var foundSpecies = false
            for (species in speciesList) {
                val representativeGenome = species.representative
                val distance = genomeCompatibility.calculateDistance(genome, representativeGenome)
                if (distance < compatibilityThreshold) {
                    species.members.add(genome)
                    foundSpecies = true
                    break
                }
            }
            if (!foundSpecies) {
                speciesList.add(Species(speciesList.size + 1, mutableListOf(genome), genome))
            }
        }
    }
    override fun updateSpeciesRepresentatives() {
        for (species in speciesList) {
            species.representative = species.members.random()
        }
    }
}
