package coevolution

import algorithm.GenomeMutator
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import algorithm.fitnessevaluator.SensorInputGenerator
import algorithm.fitnessevaluator.EnhancedStateEncoderDecoder
import algorithm.network.NetworkProcessor
import environment.*
import genome.NetworkGenome
import kotlin.random.Random

interface Agent<M, E> {
    fun mutate(potentialMates: List<Agent<M, E>>): Agent<M, E>
    suspend fun satisfiesMinimalCriterion(environment: Environment<E, M>): Boolean
    fun getModel(): M
}

class MazeSolverAgent(
    private val random: Random,
    private val mazeAgentCache: MazeAgentCache,
    private val mazeEnvironmentCache: MazeEnvironmentCache,
    private val genome: NetworkGenome,
    private val genomeMutator: GenomeMutator,
    private val solutionMap: SolutionMap<NetworkGenome, MazeGenome>,
    // val networkProcessorFactory: NetworkProcessorFactory,
    val stepsAllowed: Int,
    val sensorPositions : List<Pair<Int,Int>>
) : Agent<NetworkGenome, MazeGenome> {
    override fun mutate(potentialMates: List<Agent<NetworkGenome, MazeGenome>>): Agent<NetworkGenome, MazeGenome> {
        
        val mutatedGenome = if (genomeMutator.rollCrossMutation()) genomeMutator.crossMutateGenomes(genome, potentialMates.random(random).getModel()) else genomeMutator.mutateGenome(genome)
        
        return MazeSolverAgent(random, mazeAgentCache,mazeEnvironmentCache, mutatedGenome, genomeMutator, solutionMap, stepsAllowed, sensorPositions)
    }

    override suspend fun satisfiesMinimalCriterion(environment: Environment<MazeGenome, NetworkGenome>): Boolean {
        // if (environment !is MazeEnvironmentAdapter) return false
        val mazeEnvironment = environment.getModel()
        val generatedMaze = mazeEnvironmentCache.getMazeEnvironment(mazeEnvironment)
        
        return generatedMaze?.let { maze ->
            val tmazeEnvironment = TmazeEnvironment(maze, maze.width, maze.height)
        
            val result = MazeSolverTester(mazeAgentCache,  SensorInputGenerator(tmazeEnvironment, sensorPositions), tmazeEnvironment, stepsAllowed).canSolveMaze(genome)
            if (result.mazeFinished)
                solutionMap.addSolution(AgentEnvironmentPair(this, environment), result.visitedPositions)
            result.mazeFinished

        } ?: false
    }

    override fun getModel(): NetworkGenome {
        return genome
    }
}


data class MazeSolutionAttempt(
    val mazeFinished: Boolean,
    val visitedPositions: List<Position>
)

class MazeSolverTester(
    private val mazeAgentCache: MazeAgentCache,
    // private val networkProcessorFactory: NetworkProcessorFactory,
    private val sensorInputGenerator: SensorInputGenerator,
    private val environment: TmazeEnvironment,
    private val stepsAllowed: Int = 100 // Default steps allowed to solve the maze
) {

    fun canSolveMaze(genome: NetworkGenome): MazeSolutionAttempt {
        environment.reset()
        val mazeBoundaries = Pair(Position(0, 0), Position(environment.width - 1, environment.height - 1))
        val enhancedStateEncoderDecoder =
            EnhancedStateEncoderDecoder(mazeBoundaries, sensorInputGenerator)
        val networkProcessor = mazeAgentCache.getNetworkProcessor(genome)
        val visitedPositions = mutableListOf<Position>()
        
        for (step in 0 until stepsAllowed) {
            
            val inputs = enhancedStateEncoderDecoder.encodeAgentState(
                environment.agentPosition,
                environment.goalPosition
            )
            val output = networkProcessor.feedforward(inputs)
            val action = enhancedStateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward, position) = environment.step(action)
            if (reachedGoal) return MazeSolutionAttempt(true, visitedPositions) // Successfully solved the maze
            visitedPositions.add(position)
        }

        return MazeSolutionAttempt(false, visitedPositions) // Failed to solve the maze within the allotted steps
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

