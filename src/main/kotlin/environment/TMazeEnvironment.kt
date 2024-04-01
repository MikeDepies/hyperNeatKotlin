package environment

import kotlin.random.Random

enum class RewardSide {
    LEFT, RIGHT, CENTER, RANDOM
}

enum class Action {
    MOVE_FORWARD, MOVE_LEFT, MOVE_RIGHT
}

data class Position(val x: Int, val y: Int)
data class MazeEnvironment(val agentPosition: Position, val goalPosition: Position, val mazeStructure: Set<Position>, val rewardSide: RewardSide)
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

    return MazeEnvironment(agentPosition, goalPosition, mazeStructure, rewardSide)
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


class TmazeEnvironment(val environment: MazeEnvironment) {
    var agentPosition = environment.agentPosition // Starting position at the base of the T
    val goalPosition = environment.goalPosition
    val mazeStructure = environment.mazeStructure

    fun reset() {
        agentPosition = environment.agentPosition
    }

    fun step(action: Action): Pair<Boolean, Double> {
        val newPosition = when (action) {
            Action.MOVE_FORWARD -> agentPosition.copy(y = agentPosition.y + 1)
            Action.MOVE_LEFT -> agentPosition.copy(x = agentPosition.x - 1)
            Action.MOVE_RIGHT -> agentPosition.copy(x = agentPosition.x + 1)
        }

        // Check if the new position is within the maze structure
        if (newPosition !in mazeStructure) {
            agentPosition = newPosition
        }

        val reachedGoal = agentPosition == goalPosition
        val reward = calculateReward(reachedGoal)

        return Pair(reachedGoal, reward)
    }

    private fun calculateReward(reachedGoal: Boolean): Double = if (reachedGoal) 1.0 else -0.1
}
fun renderEnvironmentAsString(mazeEnvironment: TmazeEnvironment): String {
    val boundaries = deriveMazeBoundaries(mazeEnvironment.environment)
    val renderedMaze = StringBuilder()
    for (y in boundaries.first.y..boundaries.second.y) {
        for (x in boundaries.first.x..boundaries.second.x) {
            val currentPosition = Position(x, y)
            when {
                currentPosition == mazeEnvironment.agentPosition -> renderedMaze.append('A')
                currentPosition == mazeEnvironment.goalPosition -> renderedMaze.append('G')
                currentPosition in mazeEnvironment.mazeStructure -> renderedMaze.append('#')
                else -> renderedMaze.append(' ')
            }
        }
        renderedMaze.append('\n')
    }
    return renderedMaze.toString()
}
fun deriveMazeBoundaries(mazeEnvironment: MazeEnvironment): Pair<Position, Position> {
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var maxY = Int.MIN_VALUE

    val allPositions = mazeEnvironment.mazeStructure + setOf(mazeEnvironment.agentPosition, mazeEnvironment.goalPosition)
    for (position in allPositions) {
        if (position.x < minX) minX = position.x
        if (position.x > maxX) maxX = position.x
        if (position.y < minY) minY = position.y
        if (position.y > maxY) maxY = position.y
    }

    val lowerLeftCorner = Position(minX, minY)
    val upperRightCorner = Position(maxX, maxY)

    return Pair(lowerLeftCorner, upperRightCorner)
}
fun main() {
    val mazeStructure = createTMaze(RewardSide.RIGHT, Random.Default)
    val mazeEnvironment = TmazeEnvironment(mazeStructure)
    val actions = listOf(Action.MOVE_FORWARD, Action.MOVE_LEFT, Action.MOVE_FORWARD, Action.MOVE_RIGHT, Action.MOVE_FORWARD) // Assuming Action is an enum or similar for possible actions

    actions.forEach { action ->
        mazeEnvironment.step(action)
        println(renderEnvironmentAsString(mazeEnvironment))
    }
}
fun hasPathToGoal(environment: TmazeEnvironment): Boolean {
    val visited = mutableSetOf<Position>()
    val queue = ArrayDeque<Position>()
    queue.add(environment.agentPosition)
    
    val boundaries = deriveMazeBoundaries(environment.environment)
    val minX = boundaries.first.x
    val maxX = boundaries.second.x
    val minY = boundaries.first.y
    val maxY = boundaries.second.y

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
    
    val boundaries = deriveMazeBoundaries(environment.environment)
    val minX = boundaries.first.x
    val maxX = boundaries.second.x
    val minY = boundaries.first.y
    val maxY = boundaries.second.y

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

