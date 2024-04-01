package coevolution

interface Environment {
    fun mutate(): Environment
    var resourceUsageLimit: Int
    var resourceUsageCount: Int
    fun isResourceAvailable(): Boolean
    fun satisfiesMinimalCriterion(agent: Agent): Boolean
}