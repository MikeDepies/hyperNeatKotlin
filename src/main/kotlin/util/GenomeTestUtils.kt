package util

import genome.NetworkGenome

object GenomeTestUtils {
    /**
     * Checks for overlapping connection IDs in a list of genomes.
     * @param genomes The list of genomes to check.
     * @return The count of genomes with overlapping connection IDs.
     */
    fun countGenomesWithOverlappingConnectionIds(genomes: List<NetworkGenome>): Int {
        var count = 0
        genomes.forEach { genome ->
            val connectionIds = genome.connectionGenes.map { it.id }
            val uniqueIds = connectionIds.toSet()
            if (connectionIds.size != uniqueIds.size) {
                count++
                // println("Genome ${genome} has overlapping connection IDs.")
            }
        }
        return count
    }

    /**
     * Generates a set of all conflicted connection IDs across a list of genomes.
     * @param genomes The list of genomes to examine.
     * @return A set containing all connection IDs that appear more than once in any genome.
     */
    fun generateConflictedConnectionIds(genomes: List<NetworkGenome>): Set<Int> {
        val conflictedIds = mutableSetOf<Int>()
        genomes.forEach { genome ->
            val connectionIds = genome.connectionGenes.map { it.id }
            val uniqueIds = connectionIds.toSet()
            if (connectionIds.size != uniqueIds.size) {
                val counts = connectionIds.groupingBy { it }.eachCount()
                counts.filter { it.value > 1 }.keys.forEach { conflictedId ->
                    conflictedIds.add(conflictedId)
                }
            }
        }
        return conflictedIds
    }
}