package environment

interface MazeSolvableTester {
    fun isSolvable(maze: MazeEnvironment): Boolean
}

class BFSMazeSolvableTester : MazeSolvableTester {
    override fun isSolvable(maze: MazeEnvironment): Boolean {
        val visited = mutableSetOf<Position>()
        val queue = ArrayDeque<Position>()
        queue.add(maze.agentPosition)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current == maze.goalPosition) {
                return true // Goal found
            }

            visited.add(current)

            // Add all adjacent positions that are not walls and have not been visited yet
            getAdjacentPositions(current).forEach { adjacent ->
                if (!maze.mazeStructure.contains(adjacent) && !visited.contains(adjacent)) {
                    queue.add(adjacent)
                }
            }
        }

        return false // Goal not reachable
    }

    private fun getAdjacentPositions(position: Position): List<Position> {
        return listOf(
            Position(position.x - 1, position.y),
            Position(position.x + 1, position.y),
            Position(position.x, position.y - 1),
            Position(position.x, position.y + 1)
        )
    }
}