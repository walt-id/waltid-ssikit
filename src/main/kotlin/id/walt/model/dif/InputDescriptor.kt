package id.walt.model.dif

data class VpSchema (
    val uri: String
)

data class InputDescriptor (
    val id: String,
    val schema: VpSchema
)
