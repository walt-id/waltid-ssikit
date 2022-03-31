package id.walt.model.dif

data class VCSchema (
    val uri: String
)

data class InputDescriptor (
    val id: String,
    val name: String? = null,
    val purpose: String? = null,
    val format: VCFormat? = null,
    val constraints: InputDescriptorConstraints? = null,
    val schema: VCSchema? = null, // backwards compatibility with PresentationExchange 1.0 (https://identity.foundation/presentation-exchange/spec/v1.0.0/#input-descriptor-object)
)
