package id.walt.model.dif

import com.beust.klaxon.Json

data class CredentialManifest(
    val issuer: Issuer,
    @Json(name = "output_descriptors")
    val outputDescriptors: List<OutputDescriptor>,
    @Json(name = "presentation_definition", serializeNull = false)
    val presentationDefinition: PresentationDefinition? = null
)
