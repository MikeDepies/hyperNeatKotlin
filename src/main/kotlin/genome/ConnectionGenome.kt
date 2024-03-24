package genome

data class ConnectionGenome(val id: Int, val inputNode: NodeGenome, val outputNode: NodeGenome, val weight: Double, val enabled: Boolean)