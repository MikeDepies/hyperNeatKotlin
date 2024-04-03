package coevolution

import environment.Position
data class AgentSolution<A, E>(val agent: Agent<A, E>, val solution: List<Position>)
class SolutionMap<A, E> {
    private val map: MutableMap<Environment<E, A>, AgentSolution<A, E>> = mutableMapOf()

    fun addSolution(agent: AgentEnvironmentPair<A, E>, solution: List<Position>) {
        map[agent.environment] = AgentSolution(agent.agent, solution)
    }

    fun getSolution(environment: Environment<E, A>): AgentSolution<A, E>? {
        return map[environment]
    }

    fun clearSolutions() {
        map.clear()
    }
}
