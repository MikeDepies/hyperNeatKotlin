package genome

data class NetworkGenome(val nodeGenomes : List<NodeGenome>, val connectionGenes : List<ConnectionGenome>, var fitness : Double? = null, var sharedFitness: Double? = null, var speciesId: Int? = null)

fun NetworkGenome.render(): String {
    val builder = StringBuilder()
    builder.append("Network Genome:\n")
    builder.append("Nodes:\n")
    nodeGenomes.forEach { node ->
        builder.append(" - Node ID: ${node.id}, Type: ${node.type}, Activation Function: ${node.activationFunction}, Bias: ${node.bias}\n")
    }
    builder.append("Connections:\n")
    connectionGenes.forEach { connection ->
        builder.append(" - Connection ID: ${connection.id}, Input Node ID: ${connection.inputNode.id}, Output Node ID: ${connection.outputNode.id}, Weight: ${connection.weight}, Enabled: ${connection.enabled}\n")
    }
    fitness?.let { builder.append("Fitness: $it\n") }
    sharedFitness?.let { builder.append("Shared Fitness: $it\n") }
    speciesId?.let { builder.append("Species ID: $it\n") }
    return builder.toString()
}


