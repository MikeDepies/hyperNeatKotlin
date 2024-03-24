package com.github.mikedepies.population

import genome.NetworkGenome

data class Population(
    val genomes: MutableList<NetworkGenome>,
    val species: MutableList<Species>,
    var generation: Int = 0
)