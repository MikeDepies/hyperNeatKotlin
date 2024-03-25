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