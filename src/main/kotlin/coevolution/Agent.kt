package coevolution

interface Agent {
    fun mutate(): Agent
    fun satisfiesMinimalCriterion(environment: Environment): Boolean
}