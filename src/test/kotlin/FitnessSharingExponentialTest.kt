import algorithm.evolve.FitnessSharingExponential
import io.kotest.core.spec.style.BehaviorSpec
import population.Species
import genome.NetworkGenome

import io.kotest.matchers.shouldBe

class FitnessSharingExponentialTest : BehaviorSpec({

    given("a FitnessSharingExponential instance") {
        val fitnessSharing = FitnessSharingExponential()

        `when`("sharing fitness within an empty species") {
            val emptySpecies = Species(0, mutableListOf(), NetworkGenome(emptyList(), emptyList()))
            fitnessSharing.shareFitnessWithinSpecies(emptySpecies)
            then("all members should have a sharedFitness of 0.0") {
                emptySpecies.members.forEach { it.sharedFitness shouldBe 0.0 }
            }
        }

        `when`("sharing fitness within a species with uniform fitness values") {
            val uniformFitnessSpecies = Species(1, mutableListOf(
                NetworkGenome(emptyList(), emptyList(), fitness = 100.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 100.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 100.0)
            ), NetworkGenome(emptyList(), emptyList()))
            fitnessSharing.shareFitnessWithinSpecies(uniformFitnessSpecies)
            then("all members should have the same sharedFitness value") {
                val sharedFitnessValues = uniformFitnessSpecies.members.map { it.sharedFitness }.toSet()
                sharedFitnessValues.size shouldBe 1
            }
        }

        `when`("sharing fitness within a species with varied fitness values") {
            val variedFitnessSpecies = Species(2, mutableListOf(
                NetworkGenome(emptyList(), emptyList(), fitness = 50.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 100.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 150.0)
            ), NetworkGenome(emptyList(), emptyList()))
            fitnessSharing.shareFitnessWithinSpecies(variedFitnessSpecies)
            then("members should have correctly adjusted sharedFitness values") {
                val expectedSharedFitnessValues = listOf(
                    Math.exp(50.0),
                    Math.exp(100.0),
                    Math.exp(150.0)
                ).let { expValues ->
                    val sum = expValues.sum() / expValues.size
                    expValues.map { it / sum }
                }
                variedFitnessSpecies.members.zip(expectedSharedFitnessValues).forEach { (member, expected) ->
                    member.sharedFitness shouldBe expected
                }
            }
        }
    }
})