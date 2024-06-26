package environment

import kotlin.random.Random

enum class RewardSide {
    LEFT, RIGHT, CENTER, RANDOM
}

enum class Action {
    MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT, MOVE_BACKWARD
}

data class Position(val x: Int, val y: Int)
data class MazeEnvironment(val agentPosition: Position, val goalPosition: Position, val mazeStructure: Set<Position>, val rewardSide: RewardSide, val width: Int, val height: Int)
fun createMazeEnvironmentFromFormattedString(mazeString: String, rewardSide: RewardSide): MazeEnvironment {
    val rows = mazeString.trim().lines()
    val mazeStructure = mutableSetOf<Position>()
    var agentPosition = Position(0, 0)
    var goalPosition = Position(0, 0)

    for (y in rows.indices) {
        for (x in rows[y].indices) {
            when (rows[y][x]) {
                'A' -> agentPosition = Position(x, y)
                'G' -> goalPosition = Position(x, y)
                '#' -> mazeStructure.add(Position(x, y))
            }
        }
    }
    val width = rows[0].length
    val height = rows.size
    return MazeEnvironment(agentPosition, goalPosition, mazeStructure, rewardSide, width, height)
}
fun createTMaze(rewardSide: RewardSide, random: Random): MazeEnvironment {
    val mazeString = when (rewardSide) {
        RewardSide.LEFT -> """
            |#########
            |#       #
            |#   A   #
            |#   #   #
            |#G  #   #
            |#########
        """.trimMargin()
        RewardSide.RIGHT -> """
            |#########
            |#       #
            |#   A   #
            |#   #   #
            |#   #  G#
            |#########
        """.trimMargin()
        RewardSide.CENTER -> """
            |#########
            |#       #
            |#   A   #
            |#   #   #
            |# G #   #
            |#########
        """.trimMargin()
        RewardSide.RANDOM -> {
            val goalPosition = when ((1..6).random(random)) {
                1 -> "G  #"
                2 -> "#  G"
                3 -> "# G "
                4 -> " G #"
                5 -> "G  #"
                6 -> " G  "
                else -> error("Invalid random number")
            }
            val agentPosition = when ((1..3).random(random)) {
                1 -> "   A  " // Original position
                2 -> "A     " // Leftmost position
                else -> "     A" // Rightmost position
            }
            """
                |#########
                |#$agentPosition #
                |#   #   #
                |#   #   #
                |#$goalPosition   #
                |#########
            """.trimMargin()
        }
    }
    return createMazeEnvironmentFromFormattedString(mazeString, rewardSide)
}

data class MazeSolution(val reachedGoal: Boolean, val reward: Double, val agentPosition: Position)
class TmazeEnvironment(val environment: MazeEnvironment, val width: Int, val height: Int) {
    var agentPosition = environment.agentPosition.copy() // Starting position at the base of the T
    val goalPosition = environment.goalPosition.copy()
    val mazeStructure = environment.mazeStructure

    fun reset() {
        agentPosition = environment.agentPosition.copy()
    }

    fun step(action: Action): MazeSolution {
        val newPosition = when (action) {
            Action.MOVE_FORWARD -> agentPosition.copy(y = agentPosition.y + 1)
            Action.MOVE_LEFT -> agentPosition.copy(x = agentPosition.x - 1)
            Action.MOVE_RIGHT -> agentPosition.copy(x = agentPosition.x + 1)
            Action.MOVE_BACKWARD -> agentPosition.copy(y = agentPosition.y - 1)
        }

        val isStepValid = kotlin.math.abs(newPosition.x - agentPosition.x) + kotlin.math.abs(newPosition.y - agentPosition.y) == 1
        if (!isStepValid) {
            println("Invalid step: Agent attempted to move more than 1 tile.")
            return MazeSolution(false, -1.0, agentPosition) // Return current position with penalty
        }

        // Check if the new position is within the maze structure and within the boundaries
        if (newPosition !in mazeStructure && newPosition.x in 0 until width && newPosition.y in 0 until height) {
            agentPosition = newPosition
        }


        val reachedGoal = agentPosition == goalPosition
        val reward = calculateReward(reachedGoal)

        return MazeSolution(reachedGoal, reward, agentPosition)
    }

    private fun calculateReward(reachedGoal: Boolean): Double = if (reachedGoal) 1.0 else -0.1
}

fun getAnsiColorCodes(): List<String> {
    return listOf(
        "\u001B[35m",  // Purple
        "\u001B[36m",  // Cyan
        "\u001B[34m",  // Blue
        "\u001B[33m",  // Yellow
        
        "\u001B[31m",   // Red
        
        "\u001B[37m",  // White
        "\u001B[30m",  // Black
        "\u001B[96m",  // Bright Cyan
        // "\u001B[95m",  // Bright Purple
        "\u001B[94m",  // Bright Blue
        "\u001B[93m",  // Bright Yellow
        "\u001B[92m",  // Bright Green
        "\u001B[91m",  // Bright Red
        "\u001B[97m",  // Bright White
        "\u001B[90m",  // Bright Black
        // "\u001B[35m",  // Purple
        // "\u001B[32m",  // Green
        
    )
}

fun renderEnvironmentAsString(mazeEnvironment: TmazeEnvironment, createBorder: Boolean = false, walkedPaths: List<List<Position>> = emptyList()): String {
    val boundaries = Pair(Position(0, 0), Position(mazeEnvironment.width - 1, mazeEnvironment.height - 1))
    val renderedMaze = StringBuilder()
    val colors = getAnsiColorCodes()
    if (createBorder) {
        // Add top border
        renderedMaze.append("*".repeat(boundaries.second.x - boundaries.first.x + 3))
        renderedMaze.append("\n")
    }

    for (y in boundaries.first.y..boundaries.second.y) {
        if (createBorder) {
            // Add left border
            renderedMaze.append("* ")
        }

        for (x in boundaries.first.x..boundaries.second.x) {
            val currentPosition = Position(x, y)
            var pathIndex = -1
            when {
                currentPosition == mazeEnvironment.agentPosition -> renderedMaze.append("\u001B[35m☻\u001B[0m") // Make the A purple
                currentPosition == mazeEnvironment.goalPosition -> renderedMaze.append("\u001B[32m☼\u001B[0m") // Make the G green
                currentPosition in mazeEnvironment.mazeStructure -> renderedMaze.append('█')
                walkedPaths.any { 
                    pathIndex++
                    it.contains(currentPosition) 
                } -> {
                    val path = walkedPaths[pathIndex]
                    val index = path.lastIndexOf(currentPosition)
                    val nextPosition = if (index < path.size - 1) path[index + 1] else mazeEnvironment.goalPosition
                    val direction = when {
                        nextPosition.x < currentPosition.x -> '←'
                        nextPosition.x > currentPosition.x -> '→'
                        nextPosition.y < currentPosition.y -> '↑'
                        else -> '↓'
                    }
                    renderedMaze.append("${colors[pathIndex.coerceAtMost(colors.size - 1)]}$direction\u001B[0m") // Make the direction yellow
                }
                else -> renderedMaze.append(' ')
            }
        }
        
        if (createBorder) {
            // Add right border
            renderedMaze.append(" *")
        }

        renderedMaze.append('\n')
    }

    if (createBorder) {
        // Add bottom border
        renderedMaze.append("*".repeat(boundaries.second.x - boundaries.first.x + 3))
        renderedMaze.append("\n")
    }

    return renderedMaze.toString()
}
// fun deriveMazeBoundaries(mazeEnvironment: MazeEnvironment): Pair<Position, Position> {
    

//     return Pair(Position(0, 0), Position(mazeEnvironment.width - 1, mazeEnvironment.height - 1))
// }
// fun main() {
//     val mazeStructure = createTMaze(RewardSide.RIGHT, Random.Default)
//     val mazeEnvironment = TmazeEnvironment(mazeStructure, 10, 10)
//     val actions = listOf(Action.MOVE_FORWARD, Action.MOVE_LEFT, Action.MOVE_FORWARD, Action.MOVE_RIGHT, Action.MOVE_FORWARD) // Assuming Action is an enum or similar for possible actions

//     actions.forEach { action ->
//         mazeEnvironment.step(action)
//         println(renderEnvironmentAsString(mazeEnvironment))
//     }
// }
fun hasPathToGoal(environment: TmazeEnvironment): Boolean {
    val visited = mutableSetOf<Position>()
    val queue = ArrayDeque<Position>()
    queue.add(environment.agentPosition)
    
    
    val minX = 0
    val maxX = environment.width - 1
    val minY = 0
    val maxY = environment.height - 1

    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        if (current == environment.goalPosition) {
            return true
        }

        val neighbors = listOf(
            Position(current.x + 1, current.y).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x - 1, current.y).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x, current.y + 1).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x, current.y - 1).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY }
        ).filterNotNull()

        neighbors.filter { neighbor ->
            neighbor !in environment.mazeStructure && neighbor !in visited
        }.forEach { neighbor ->
            visited.add(neighbor)
            queue.add(neighbor)
        }
    }

    return false
}
fun shortestPathToGoal(environment: TmazeEnvironment): Int {
    val visited = mutableSetOf<Position>()
    val queue = ArrayDeque<Pair<Position, Int>>()
    queue.add(Pair(environment.agentPosition, 0))
    
    
    val minX = 0
    val maxX = environment.width - 1
    val minY = 0
    val maxY = environment.height - 1

    while (queue.isNotEmpty()) {
        val (current, distance) = queue.removeFirst()
        if (current == environment.goalPosition) {
            return distance
        }

        val neighbors = listOf(
            Position(current.x + 1, current.y).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x - 1, current.y).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x, current.y + 1).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY },
            Position(current.x, current.y - 1).takeIf { it.x <= maxX && it.y <= maxY && it.x >= minX && it.y >= minY }
        ).filterNotNull()

        neighbors.filter { neighbor ->
            neighbor !in environment.mazeStructure && neighbor !in visited
        }.forEach { neighbor ->
            visited.add(neighbor)
            queue.add(Pair(neighbor, distance + 1))
        }
    }

    return -1 // Return -1 if no path to goal is found
}

