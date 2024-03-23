package com.github.mikedepies

interface FitnessEvaluator {
    fun calculateFitness(genome: NetworkGenome): Double
}