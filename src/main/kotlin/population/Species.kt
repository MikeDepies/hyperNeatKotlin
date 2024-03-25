package population

import genome.NetworkGenome

data class Species(
    val id: Int,
    val members: MutableList<NetworkGenome>,
    var representative: NetworkGenome,
    // var averageFitness: Double,
    var staleness: Int = 0
)