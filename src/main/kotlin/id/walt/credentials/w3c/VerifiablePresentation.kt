package id.walt.credentials.w3c

import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.builder.CredentialFactory
import kotlinx.serialization.json.*

class VerifiablePresentation internal constructor(jsonObject: JsonObject): VerifiableCredential(jsonObject) {
    val holder: String?
        get() = properties["holder"]?.toString()
    val verifiableCredential: List<VerifiableCredential>?
        get() = (properties["verifiableCredential"] as? List<*>)
            ?.map { JsonConverter.toJsonElement(it) }
            ?.map {
                when(it) {
                    is JsonPrimitive -> fromString(it.content)
                    is JsonObject -> fromJsonObject(it)
                    else -> throw Exception("Invalid type of verifiableCredential item")
                }
            }

    companion object: CredentialFactory<VerifiablePresentation> {
        override fun fromJsonObject(jsonObject: JsonObject) = VerifiablePresentation(jsonObject)
    }
}

class VerifiablePresentationBuilder: AbstractW3CCredentialBuilder<VerifiablePresentation, VerifiablePresentationBuilder>(listOf("VerifiablePresentation"), VerifiablePresentation) {
    fun setHolder(holder: String) = setProperty("holder", holder)
    fun setVerifiableCredentials(verifiableCredentials: List<VerifiableCredential>) = setProperty("verifiableCredential", verifiableCredentials.map { it.toJsonElement() }.toList())
}
