import algorithm.evolve.FitnessSharing
import algorithm.evolve.FitnessSharingAverage
import population.Species
import genome.NetworkGenome
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldNotThrowAny

class FitnessSharingTest : BehaviorSpec({
    given("a species with network genomes") {
        val initialGenomes = mutableListOf(
            NetworkGenome(emptyList(), emptyList(), fitness = 100.0),
            NetworkGenome(emptyList(), emptyList(), fitness = 200.0)
        )
        val species = Species(1, initialGenomes, initialGenomes.first())
        `when`("shareFitnessWithinSpecies is invoked") {
           val fitnessSharing = FitnessSharingAverage()
           fitnessSharing.shareFitnessWithinSpecies(species)
           then("it should share fitness within species correctly") {
               species.members[0].sharedFitness shouldBe 50.0
               species.members[1].sharedFitness shouldBe 100.0
           }
        }

        
        `when`("the species has no genomes") {
            val emptyGenomes = mutableListOf<NetworkGenome>()
            val speciesWithNoGenomes = Species(2, emptyGenomes, NetworkGenome(emptyList(), emptyList()))
            val fitnessSharing = FitnessSharingAverage()
            then("it should handle the species without throwing exceptions") {
                shouldNotThrowAny {
                    fitnessSharing.shareFitnessWithinSpecies(speciesWithNoGenomes)
                }
            }
        }

        `when`("some genomes have negative fitness values") {
            val genomesWithNegativeFitness = mutableListOf(
                NetworkGenome(emptyList(), emptyList(), fitness = -100.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 200.0)
            )
            val speciesWithNegativeFitness = Species(3, genomesWithNegativeFitness, genomesWithNegativeFitness.first())
            val fitnessSharing = FitnessSharingAverage()
            fitnessSharing.shareFitnessWithinSpecies(speciesWithNegativeFitness)
            then("it should correctly share fitness among genomes") {
                speciesWithNegativeFitness.members[0].sharedFitness shouldBe -50.0
                speciesWithNegativeFitness.members[1].sharedFitness shouldBe 100.0
            }
        }
        `when`("the species has a large number of genomes") {
            val largeNumberOfGenomes = MutableList(1000) { 
                NetworkGenome(emptyList(), emptyList(), fitness = (it + 1).toDouble()) 
            }
            val speciesWithLargeNumberOfGenomes = Species(4, largeNumberOfGenomes, largeNumberOfGenomes.first())
            val fitnessSharing = FitnessSharingAverage()
            fitnessSharing.shareFitnessWithinSpecies(speciesWithLargeNumberOfGenomes)
            then("it should properly share fitness within the species") {
                speciesWithLargeNumberOfGenomes.members.forEachIndexed { index, genome ->
                    genome.sharedFitness shouldBe (index + 1).toDouble() / 1000
                }
            }
        }
        `when`("some genomes have zero fitness values") {
            val genomesWithZeroFitness = mutableListOf(
                NetworkGenome(emptyList(), emptyList(), fitness = 0.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 200.0)
            )
            val speciesWithZeroFitness = Species(5, genomesWithZeroFitness, genomesWithZeroFitness.first())
            val fitnessSharing = FitnessSharingAverage()
            fitnessSharing.shareFitnessWithinSpecies(speciesWithZeroFitness)
            then("it should correctly handle zero fitness values without errors") {
                speciesWithZeroFitness.members[0].sharedFitness shouldBe 0.0
                speciesWithZeroFitness.members[1].sharedFitness shouldBe 100.0
            }
        }
        `when`("fitness sharing is applied across multiple species") {
            val speciesList = listOf(
                Species(1, mutableListOf(NetworkGenome(emptyList(), emptyList(), fitness = 100.0)), NetworkGenome(emptyList(), emptyList())),
                Species(2, mutableListOf(NetworkGenome(emptyList(), emptyList(), fitness = 200.0)), NetworkGenome(emptyList(), emptyList()))
            )
            val fitnessSharing = FitnessSharingAverage()
            speciesList.forEach { species ->
                fitnessSharing.shareFitnessWithinSpecies(species)
            }
            then("it should isolate fitness sharing to individual species") {
                speciesList[0].members[0].sharedFitness shouldBe 100.0
                speciesList[1].members[0].sharedFitness shouldBe 200.0
            }
        }
        `when`("shared fitness calculation is performed multiple times on the same set of genomes") {
            val genomes = mutableListOf(
                NetworkGenome(emptyList(), emptyList(), fitness = 100.0),
                NetworkGenome(emptyList(), emptyList(), fitness = 200.0)
            )
            val species = Species(6, genomes, genomes.first())
            val fitnessSharing = FitnessSharingAverage()
            // First calculation
            fitnessSharing.shareFitnessWithinSpecies(species)
            val firstCalculationSharedFitnesses = species.members.map { it.sharedFitness }
            // Second calculation
            fitnessSharing.shareFitnessWithinSpecies(species)
            val secondCalculationSharedFitnesses = species.members.map { it.sharedFitness }
            
            then("it should produce consistent shared fitness values") {
                firstCalculationSharedFitnesses.zip(secondCalculationSharedFitnesses).forEach { (first, second) ->
                    first shouldBe second
                }
            }
        }

        
    }
})


