package algorithm

import genome.NetworkGenome

interface GenomeMutator {
    fun mutateGenome(genome: NetworkGenome)
}
