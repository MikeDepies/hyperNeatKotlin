package genome

data class NodeGenome(val id: Int, val type: NodeType, var activationFunction: ActivationFunction, val bias: Double)