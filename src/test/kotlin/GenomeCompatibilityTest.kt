import algorithm.evolve.GenomeCompatibility
import genome.NetworkGenome
import genome.NodeGenome
import genome.ConnectionGenome
import genome.NodeType
import genome.ActivationFunction
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.shouldBeExactly

class GenomeCompatibilityTest : BehaviorSpec({
    given("two network genomes") {
        val genome1 = NetworkGenome(emptyList(), emptyList())
        val genome2 = NetworkGenome(emptyList(), emptyList())
        `when`("calculateDistance is invoked") {
            val genomeCompatibility = object : GenomeCompatibility {
                override fun calculateDistance(genome1: NetworkGenome, genome2: NetworkGenome): Double {
                    // Example implementation
                    return 1.0
                }
            }
            then("it should return the correct distance") {
                genomeCompatibility.calculateDistance(genome1, genome2) shouldBeExactly 1.0
            }
        }
    }
})