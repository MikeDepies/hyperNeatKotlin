import algorithm.network.*
import genome.*
import org.jetbrains.kotlinx.multik.api.mk
import org.jetbrains.kotlinx.multik.api.zeros
import org.jetbrains.kotlinx.multik.ndarray.data.*
import kotlin.math.absoluteValue

typealias Point = List<Double>

data class PointWrapper(val point: Point, val indexes: List<Int>)
interface PointNormalizer {
    fun normalize(point: List<Int>): Point
}

class DefaultPointNormalizer(val width: Int, val height: Int, val depth: Int) : PointNormalizer {
    override fun normalize(point: List<Int>): Point {
        val xNormalized = (point[0].toDouble() / width) * 2 - 1
        val yNormalized = (point[1].toDouble() / height) * 2 - 1
        val zNormalized = (point[2].toDouble() / depth) * 2 - 1
        return listOf(xNormalized, yNormalized, zNormalized)
    }
}

interface SubstrateFilter {
    fun filter(pointWrapper: PointWrapper): Boolean
}

class DefaultSubstrateFilter(val f: (PointWrapper) -> Boolean) : SubstrateFilter {
    override fun filter(pointWrapper: PointWrapper): Boolean {
        return f(pointWrapper)
    }
}

fun createSubstrate(
    pointNormalizer: PointNormalizer,
    width: Int,
    height: Int,
    depth: Int,
    filter: SubstrateFilter
): Substrate {
    val points: List<List<List<PointWrapper>>> = List(depth) { z ->
        List(height) { y ->
            List(width) { x ->
                val point = pointNormalizer.normalize(listOf(x, y, z))
                PointWrapper(point, listOf(x, y, z)).let {
                    if (filter.filter(it)) it else null
                }
            }.filterNotNull()
        }
    }
    return Substrate(width, height, depth, points)
}

interface NodeTypeResolver {
    fun resolveNodeType(pointWrapper: PointWrapper): NodeType
}

class DefaultNodeTypeResolver(val nodeType: (PointWrapper) -> NodeType) : NodeTypeResolver {
    override fun resolveNodeType(pointWrapper: PointWrapper): NodeType {
        return nodeType(pointWrapper)
    }
}

data class Substrate(val width: Int, val height: Int, val depth: Int, val points: List<List<List<PointWrapper>>>) {

}

// data class CPPN(val network: NetworkGenome)

data class HyperNEAT(val substrate: Substrate, val nodeTypeResolver: NodeTypeResolver)

class HyperNetworkBuilder(
    private val networkProcessorFactory: NetworkProcessorFactory,
    val connectionThreshold: Double,
    val connectionScalar: Double,
    val biasPoint: Point
) {

    fun buildHyperNeatNetwork(hyperNEAT: HyperNEAT, networkGenome: NetworkGenome): NetworkProcessor {
        var nodeIndex = 0
        val networkProcessor = networkProcessorFactory.createProcessor(networkGenome)
        val nodeGenomeMap = mutableMapOf<Point, NodeGenome>()
        // Query the network processor with the points in the substrate
        val points = hyperNEAT.substrate.points
        
        val connections = buildList {
            for ((sourceFrameIndex, sourceFramePoints) in points.withIndex()) {
                for (destinationFramePoints in points.drop(sourceFrameIndex + 1)) {
                    //sourceFramePoints represents a 2d grid of points at the z index of sourceFrameIndex
                    //destinationFramePoints represents a 2d grid of points at the z index of destinationFrameIndex
                    //For each point between these frames, the source should query the target with the network processor
                    //the network processor is expecting 6 values, 3 values for the source point, and 3 values for the target point.
                    for (sourcePoint in sourceFramePoints.flatten()) {
                        for (targetPoint in destinationFramePoints.flatten()) {

                            val weightValues = networkProcessor.feedforward(sourcePoint.point + targetPoint.point)
                            // Do something with the weight values
                            val weightValue = weightValues.first()
                            // println("Weight value: $weightValues")
                            if (weightValue.absoluteValue > connectionThreshold) {
                                // Create a connection
                                val sourceNode = nodeGenomeMap.getOrPut(sourcePoint.point) {
                                    val bias = networkProcessor.feedforward(biasPoint + targetPoint.point).first()
                                    NodeGenome(
                                        nodeIndex++,
                                        hyperNEAT.nodeTypeResolver.resolveNodeType(sourcePoint),
                                        ActivationFunction.SIGMOID,
                                        bias
                                    )
                                }
                                val targetNode = nodeGenomeMap.getOrPut(targetPoint.point) {
                                    NodeGenome(
                                        nodeIndex++,
                                        hyperNEAT.nodeTypeResolver.resolveNodeType(targetPoint),
                                        ActivationFunction.SIGMOID,
                                        0.0
                                    )
                                }
                                val connection = ConnectionGenome(0, sourceNode, targetNode, weightValue * connectionScalar, true)
                                add(connection)
                            }
                        }
                    }
                }
            }
        }
        return networkProcessorFactory.createProcessor(NetworkGenome(nodeGenomeMap.values.toList(), connections))
    }
}
