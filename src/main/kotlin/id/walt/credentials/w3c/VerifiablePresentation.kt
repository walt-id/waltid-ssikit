package id.walt.credentials.w3c

import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.builder.CredentialFactory
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class VerifiablePresentation internal constructor(jsonObject: JsonObject) : VerifiableCredential(jsonObject) {
    val holder: String?
        get() = properties["holder"]?.toString()
    val verifiableCredential: List<VerifiableCredential>?
        get() = (properties["verifiableCredential"] as? List<*>)
            ?.map { JsonConverter.toJsonElement(it) }
            ?.map {
                when (it) {
                    is JsonPrimitive -> VerifiableCredential.fromString(it.content)
                    is JsonObject -> VerifiableCredential.fromJsonObject(it)
                    else -> throw IllegalArgumentException("Invalid type of verifiableCredential item")
                }
            }

    override val issuerId: String?
        get() = holder
    override val subjectId: String?
        get() = holder

    companion object : CredentialFactory<VerifiablePresentation> {
        override fun fromJsonObject(jsonObject: JsonObject) = VerifiablePresentation(jsonObject)
        fun fromVerifiableCredential(verifiableCredential: VerifiableCredential) =
            VerifiablePresentation(verifiableCredential.toJsonObject()).apply {
                this.jwt = verifiableCredential.jwt
            }

        fun fromString(data: String) = fromVerifiableCredential(data.toVerifiableCredential())
    }
}

class VerifiablePresentationBuilder : AbstractW3CCredentialBuilder<VerifiablePresentation, VerifiablePresentationBuilder>(
    listOf("VerifiablePresentation"),
    VerifiablePresentation
) {
    fun setHolder(holder: String) = setProperty("holder", holder)
    fun setVerifiableCredentials(verifiableCredentials: List<VerifiableCredential>) =
        setProperty("verifiableCredential", verifiableCredentials.map { it.toJsonElement() }.toList())
}

fun String.toVerifiablePresentation() = VerifiablePresentation.fromString(this)
