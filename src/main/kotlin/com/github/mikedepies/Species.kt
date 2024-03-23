package com.github.mikedepies

data class Species(
    val id: Int,
    val members: MutableList<NetworkGenome>,
    var representative: NetworkGenome,
    var averageFitness: Double,
    var staleness: Int = 0
)