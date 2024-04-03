package environment

import algorithm.network.NetworkProcessor
import kotlin.math.sqrt

data class MazeThresholds(
    val wallThreshold: Double = 0.5,
    val agentStartThreshold: Double = 0.5,
    val goalPositionThreshold: Double = 0.5
)

class CPPNMazeGenerator(val mazeThresholds: MazeThresholds, private val width: Int, private val height: Int) {
    
    fun generateMaze(mazeQuery: MazeQuery): MazeEnvironment? {
        val mazeStructure = mutableSetOf<Position>()
        var agentPosition: Position? = null
        var goalPosition: Position? = null

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Normalize the input coordinates to be between -1 and 1
                val normalizedX = (x.toDouble() / width) * 2 - 1
                val normalizedY = (y.toDouble() / height) * 2 - 1
                val output = mazeQuery.query(normalizedX, normalizedY)
                val wallProbability = output[0]
                val agentStartPositionProbability = output[1]
                val goalPositionProbability = output[2]

                when {
                    wallProbability > mazeThresholds.wallThreshold -> mazeStructure.add(Position(x, y))
                    agentStartPositionProbability > mazeThresholds.agentStartThreshold && agentPosition == null -> agentPosition = Position(x, y)
                    goalPositionProbability > mazeThresholds.goalPositionThreshold && goalPosition == null -> goalPosition = Position(x, y)
                }
            }
        }

        // Check if either agentPosition or goalPosition is null before proceeding
        if (agentPosition == null || goalPosition == null) {
            return null // Return null if we failed to find either an agent or a goal position
        }

        // Ensure that agentPosition and goalPosition are not part of the maze structure
        mazeStructure.remove(agentPosition)
        mazeStructure.remove(goalPosition)

        return MazeEnvironment(agentPosition, goalPosition, mazeStructure, RewardSide.RANDOM, width, height)
    }

    private fun findFallbackPosition(mazeStructure: Set<Position>, defaultPosition: Position): Position {
        if (!mazeStructure.contains(defaultPosition)) {
            return defaultPosition
        }
        // Implement a more sophisticated fallback logic if the default position is not suitable
        return Position(0, 0) // This is a simplistic fallback. Consider improving.
    }
}

interface MazeQuery {
    fun query(x: Double, y: Double): List<Double>
}


class CPPNMazeQuery(private val networkProcessor: NetworkProcessor) : MazeQuery {
    override fun query(x: Double, y: Double): List<Double> {
        // Use the network processor to query the CPPN with the given x and y coordinates
        val distanceToCenter = sqrt(x * x + y * y)
        return networkProcessor.feedforward(listOf(x, y, distanceToCenter))
    }
}
