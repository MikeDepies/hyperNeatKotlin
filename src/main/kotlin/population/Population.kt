package population

import genome.NetworkGenome
import population.Species

data class Population(
    val genomes: MutableList<NetworkGenome>,
    val species: MutableList<Species>,
    var generation: Int = 0
)