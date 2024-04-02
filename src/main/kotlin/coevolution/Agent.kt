package coevolution

import algorithm.GenomeMutator
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import algorithm.fitnessevaluator.SensorInputGenerator
import algorithm.fitnessevaluator.EnhancedStateEncoderDecoder
import algorithm.network.NetworkProcessor
import environment.*
import genome.NetworkGenome

interface Agent<M, E> {
    fun mutate(potentialMates: List<Agent<M, E>>): Agent<M, E>
    fun satisfiesMinimalCriterion(environment: Environment<E, M>): Boolean
    fun getModel(): M
}

class MazeSolverAgent(
    private val mazeAgentCache: MazeAgentCache,
    private val mazeEnvironmentCache: MazeEnvironmentCache,
    private val genome: NetworkGenome,
    private val genomeMutator: GenomeMutator,
    // val networkProcessorFactory: NetworkProcessorFactory,
    val stepsAllowed: Int
) : Agent<NetworkGenome, MazeGenome> {
    override fun mutate(potentialMates: List<Agent<NetworkGenome, MazeGenome>>): Agent<NetworkGenome, MazeGenome> {
        
        val mutatedGenome = if (genomeMutator.rollCrossMutation()) genomeMutator.crossMutateGenomes(genome, potentialMates.random().getModel()) else genomeMutator.mutateGenome(genome)
        
        return MazeSolverAgent(mazeAgentCache,mazeEnvironmentCache, mutatedGenome, genomeMutator,  stepsAllowed)
    }

    override fun satisfiesMinimalCriterion(environment: Environment<MazeGenome, NetworkGenome>): Boolean {
        // if (environment !is MazeEnvironmentAdapter) return false
        val mazeEnvironment = environment.getModel()
        val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(mazeEnvironment)
        
        return generatedMaze?.let { maze ->
            val tmazeEnvironment = TmazeEnvironment(maze, maze.width, maze.height)
        
            MazeSolverTester(mazeAgentCache,  SensorInputGenerator(tmazeEnvironment), tmazeEnvironment, stepsAllowed).canSolveMaze(genome)
        } ?: false
    }

    override fun getModel(): NetworkGenome {
        return genome
    }
}


class MazeSolverTester(
    private val mazeAgentCache: MazeAgentCache,
    // private val networkProcessorFactory: NetworkProcessorFactory,
    private val sensorInputGenerator: SensorInputGenerator,
    private val environment: TmazeEnvironment,
    private val stepsAllowed: Int = 100 // Default steps allowed to solve the maze
) {

    fun canSolveMaze(genome: NetworkGenome): Boolean {
        environment.reset()
        val mazeBoundaries = Pair(Position(0, 0), Position(environment.width - 1, environment.height - 1))
        val enhancedStateEncoderDecoder =
            EnhancedStateEncoderDecoder(mazeBoundaries, sensorInputGenerator)
        val networkProcessor = mazeAgentCache.getNetworkProcessor(genome)
        
        for (step in 0 until stepsAllowed) {
            
            val inputs = enhancedStateEncoderDecoder.encodeAgentState(
                environment.agentPosition,
                environment.goalPosition
            )
            val output = networkProcessor.feedforward(inputs)
            val action = enhancedStateEncoderDecoder.decodeAction(output)

            val (reachedGoal, _) = environment.step(action)
            if (reachedGoal) return true // Successfully solved the maze
        }

        return false // Failed to solve the maze within the allotted steps
    }
}


class MazeAgentCache(
    private val networkProcessorFactory: NetworkProcessorFactory,
    
) {
    private val cache: MutableMap<NetworkGenome, NetworkProcessor> = mutableMapOf()

    fun getNetworkProcessor(networkGenome: NetworkGenome): NetworkProcessor {
        return cache.getOrPut(networkGenome) {
            val networkProcessor = networkProcessorFactory.createProcessor(networkGenome)
            networkProcessor
        }
    }

    fun clearCache() {
        cache.clear()
    }
}

