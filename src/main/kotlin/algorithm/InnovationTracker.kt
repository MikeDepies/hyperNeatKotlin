package algorithm
import java.util.concurrent.atomic.AtomicInteger

class InnovationTracker(private val currentInnovationNumber : AtomicInteger = AtomicInteger(0)) {
    fun getNextInnovationNumber(): Int = currentInnovationNumber.getAndIncrement()
}