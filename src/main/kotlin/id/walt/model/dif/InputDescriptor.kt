package id.walt.model.dif

data class VpSchema (
    val uri: String
)

data class InputDescriptor (
    val schema: VpSchema,
    val id: String? = null
)
