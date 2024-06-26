package algorithm.fitnessevaluator

import algorithm.FitnessEvaluator
import algorithm.network.DefaultActivationFunctionMapper
import algorithm.network.NetworkBuilder
import algorithm.network.NetworkProcessorFactory
import environment.*
import genome.NetworkGenome
import kotlin.random.Random

class TmazeFitnessEvaluator(val environment: TmazeEnvironment) : FitnessEvaluator {

    

    override fun calculateFitness(genome: NetworkGenome): Double {
        var fitness = 1.0
        val stepsAllowed = 100 // Maximum steps allowed to find the reward
        environment.reset()
        val mazeBoundaries = Pair(Position(0, 0), Position(environment.width - 1, environment.height - 1))
        val stateEncoderDecoder = StateEncoderDecoder(mazeBoundaries)
        val networkProcessor =
                NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()), false)
                        .createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs =
                    stateEncoderDecoder.encodeAgentState(
                            environment.agentPosition,
                            environment.environment.rewardSide
                    )
            val output = networkProcessor.feedforward(inputs)
            val action = stateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward) = environment.step(action)
            fitness += reward
            if (reachedGoal) break
        }

        return fitness
    }
}

class MazeFitnessEvaluatorSensor(
        val sensorInputGenerator: SensorInputGenerator,
        val environment: TmazeEnvironment
) : FitnessEvaluator {

    override fun calculateFitness(genome: NetworkGenome): Double {
        var fitness = 0.0
        val stepsAllowed = 100 // Maximum steps allowed to find the reward
        environment.reset()
        val mazeBoundaries = Pair(Position(0, 0), Position(environment.width - 1, environment.height - 1))
        val enhancedStateEncoderDecoder =
                EnhancedStateEncoderDecoder(mazeBoundaries, sensorInputGenerator)
        val networkProcessor =
                NetworkProcessorFactory(NetworkBuilder(DefaultActivationFunctionMapper()), false)
                        .createProcessor(genome)

        for (step in 0 until stepsAllowed) {
            val inputs =
                    enhancedStateEncoderDecoder.encodeAgentState(
                            environment.agentPosition,
                            environment.goalPosition
                    )
            val output = networkProcessor.feedforward(inputs)
            val action = enhancedStateEncoderDecoder.decodeAction(output)

            val (reachedGoal, reward) = environment.step(action)
            fitness += reward
            if (reachedGoal) break
        }

        return fitness
    }
}

fun createSensorPositions(sensorRange: Int): List<Position> {
    val directions = mutableListOf<Position>()
    for (y in -sensorRange..sensorRange) {
        for (x in -sensorRange..sensorRange) {
            if (x != 0 || y != 0) {
                directions.add(Position(x, y))
            }
        }
    }
    return directions
}
class SensorInputGenerator(val environment: TmazeEnvironment, private val directions: List<Position>) {

    fun generateSensorData(): List<Double> {
        return directions.map { (dx, dy) ->
            val checkPosition =
                    environment.agentPosition.copy(
                            x = environment.agentPosition.x + dx,
                            y = environment.agentPosition.y + dy
                    )
            when {
                checkPosition in environment.mazeStructure -> 1.0 // Wall
                checkPosition == environment.goalPosition -> 0.5 // Goal position
                else -> 0.0 // Empty space
            }
        }
    }
}



class EnhancedStateEncoderDecoder(
        private val mazeBoundaries: Pair<Position, Position>,
        private val sensorInputGenerator: SensorInputGenerator
) {

    fun encodeAgentState(agentPosition: Position, goalPosition: Position): List<Double> {
        // Encoding the actual agent's position, the reward side, the goal position, the maze boundaries, and sensor data into
        // a list of doubles
        val mazeWidth = mazeBoundaries.second.x.toDouble() - mazeBoundaries.first.x.toDouble() + 1.0
        val mazeHeight = mazeBoundaries.second.y.toDouble() - mazeBoundaries.first.y.toDouble() + 1.0
        val normalizedAgentX = (agentPosition.x.toDouble() - mazeBoundaries.first.x) / mazeWidth
        val normalizedAgentY = (agentPosition.y.toDouble() - mazeBoundaries.first.y) / mazeHeight
        val normalizedGoalX = (goalPosition.x.toDouble() - mazeBoundaries.first.x) / mazeWidth
        val normalizedGoalY = (goalPosition.y.toDouble() - mazeBoundaries.first.y) / mazeHeight
        val encodedAgentPosition = listOf(normalizedAgentX, normalizedAgentY)
        val encodedGoalPosition = listOf(normalizedGoalX, normalizedGoalY)
        
        val sensorData = sensorInputGenerator.generateSensorData()

        return encodedAgentPosition + encodedGoalPosition + sensorData
    }

    fun decodeAction(output: List<Double>): Action {
        // Decoding the network's output into an action
        val maxIndex = output.indexOf(output.maxOrNull() ?: 0.0)
        return when (maxIndex) {
            0 -> Action.MOVE_FORWARD
            1 -> Action.MOVE_LEFT
            2 -> Action.MOVE_RIGHT
            3 -> Action.MOVE_BACKWARD
            else -> Action.MOVE_FORWARD // Default action
        }
    }
}

class StateEncoderDecoder(val mazeBoundaries: Pair<Position, Position>) {

    fun encodeAgentState(agentPosition: Position, rewardSide: RewardSide): List<Double> {
        // Encoding the agent's position, the reward side, and the maze boundaries into a list of
        // doubles
        val mazeWidth = mazeBoundaries.second.x.toDouble() - mazeBoundaries.first.x.toDouble() + 1.0
        val mazeHeight =
                mazeBoundaries.second.y.toDouble() - mazeBoundaries.first.y.toDouble() + 1.0
        val normalizedX = (agentPosition.x.toDouble() - mazeBoundaries.first.x) / mazeWidth
        val normalizedY = (agentPosition.y.toDouble() - mazeBoundaries.first.y) / mazeHeight
        val encodedPosition = listOf(normalizedX, normalizedY)
        val encodedRewardSide =
                when (rewardSide) {
                    RewardSide.LEFT -> 0.0
                    RewardSide.RIGHT -> 1.0
                    RewardSide.CENTER -> 0.5
                    RewardSide.RANDOM -> -1.0
                }
        return encodedPosition + encodedRewardSide
    }

    fun decodeAction(output: List<Double>): Action {
        // Decoding the network's output into an action
        // Assuming the output is a list of doubles representing probabilities for each action
        val maxIndex = output.indexOf(output.maxOrNull() ?: 0.0)
        return when (maxIndex) {
            0 -> Action.MOVE_FORWARD
            1 -> Action.MOVE_LEFT
            2 -> Action.MOVE_RIGHT
            3 -> Action.MOVE_BACKWARD
            else -> Action.MOVE_FORWARD // Default action
        }
    }
}

