package ai.sunnystratgies.com.github.mikedepies

class InnovationTracker(private var currentInnovationNumber : Int = 0) {
    fun getNextInnovationNumber(): Int = currentInnovationNumber++
}