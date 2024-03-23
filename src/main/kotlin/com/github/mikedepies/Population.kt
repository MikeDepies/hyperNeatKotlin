package com.github.mikedepies

data class Population(
    val genomes: MutableList<NetworkGenome>,
    val species: MutableList<Species>,
    var generation: Int = 0
)