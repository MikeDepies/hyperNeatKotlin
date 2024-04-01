package coevolution


import environment.*
import algorithm.network.NetworkProcessorFactory
import genome.NetworkGenome
import algorithm.GenomeMutator
interface Environment {
    fun mutate(): Environment
    val resourceUsageLimit: Int
    var resourceUsageCount: Int
    fun isResourceAvailable(): Boolean
    fun satisfiesMinimalCriterion(agent: Agent): Boolean
}




data class MazeGenome(
    val networkGenome: NetworkGenome,
    val mazeThresholds: MazeThresholds,
    val width: Int,
    val height: Int
)

interface MazeGenomeMutator {
    fun mutate(mazeGenome: MazeGenome): MazeGenome
}
class SimpleMazeGenomeMutator(private val genomeMutator: GenomeMutator) : MazeGenomeMutator {
    override fun mutate(mazeGenome: MazeGenome): MazeGenome {
        // Mutate the networkGenome using the provided genomeMutator
        val mutatedNetworkGenome = genomeMutator.mutateGenome(mazeGenome.networkGenome)

        // Assuming mutation involves slight adjustments to the maze thresholds, width, and height
        val mutatedMazeThresholds = mazeGenome.mazeThresholds.copy(
            wallThreshold = mazeGenome.mazeThresholds.wallThreshold * (0.9 + (Math.random() * 0.2)), // Randomly adjust by -10% to +10%
            goalPositionThreshold = mazeGenome.mazeThresholds.goalPositionThreshold * (0.9 + (Math.random() * 0.2)),
            agentStartThreshold = mazeGenome.mazeThresholds.agentStartThreshold * (0.9 + (Math.random() * 0.2))
        )
        val mutatedWidth = (mazeGenome.width * (0.95 + (Math.random() * 0.1))).toInt() // Randomly adjust width by -5% to +5%
        val mutatedHeight = (mazeGenome.height * (0.95 + (Math.random() * 0.1))).toInt() // Randomly adjust height by -5% to +5%

        // Return a new MazeGenome instance with the mutated properties, including the mutated networkGenome
        return mazeGenome.copy(
            networkGenome = mutatedNetworkGenome,
            mazeThresholds = mutatedMazeThresholds,
            width = mutatedWidth,
            height = mutatedHeight
        )
    }
}

class MazeEnvironmentAdapter(
    private val genomeMutator: GenomeMutator,
    private val networkProcessorFactory: NetworkProcessorFactory,
    private val mazeGenome: MazeGenome,
    override val resourceUsageLimit: Int,
    override var resourceUsageCount: Int = 0
) : Environment {

    override fun mutate(): Environment {
        // Generate a mutated genome
        val mutatedGenome = genomeMutator.mutateGenome(mazeGenome.networkGenome)
        
        // Return a new instance of MazeEnvironmentAdapter with the mutated genome
        return MazeEnvironmentAdapter(genomeMutator, networkProcessorFactory, MazeGenome(mutatedGenome, mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height), resourceUsageLimit)
    }

    override fun isResourceAvailable(): Boolean {
        // Example resource check: Ensure we haven't exceeded the usage limit
        return resourceUsageCount < resourceUsageLimit
    }

    override fun satisfiesMinimalCriterion(agent: Agent): Boolean {
        // Create a new NetworkProcessor from the genome
        val networkProcessor = networkProcessorFactory.createProcessor(mazeGenome.networkGenome)
        
        // Use the NetworkProcessor to create a CPPNMazeQuery
        val cppnMazeQuery = CPPNMazeQuery(networkProcessor)
        
        // Generate a new maze environment using the CPPNMazeQuery
        val cppnMazeGenerator = CPPNMazeGenerator(mazeGenome.mazeThresholds, mazeGenome.width, mazeGenome.height)
        val mazeEnvironment = cppnMazeGenerator.generateMaze(cppnMazeQuery)
        
        // Check if the generated maze environment satisfies the minimal criterion
        // For example, ensuring there's a path from the agent to the goal
        return mazeEnvironment?.let { env ->
            val tmazeEnvironment = TmazeEnvironment(env)
            hasPathToGoal(tmazeEnvironment)
        } ?: false
    }
}