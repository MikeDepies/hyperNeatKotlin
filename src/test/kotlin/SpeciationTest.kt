import algorithm.evolve.Speciation
import genome.NetworkGenome
import io.kotest.core.spec.style.BehaviorSpec

//class SpeciationTest : BehaviorSpec({
//    given("a list of network genomes") {
//        val genomes = listOf(NetworkGenome(emptyList(), emptyList()))
//        `when`("categorizeIntoSpecies is invoked") {
//            val speciation = object : Speciation {
//                override fun categorizeIntoSpecies(genomes: List<NetworkGenome>) {
//                    // Example implementation
//                }
//                override fun updateSpeciesRepresentatives() {
//                    // Example implementation
//                }
//            }
//            then("it should categorize genomes correctly") {
//                // Assertions
//            }
//        }
//    }
//})