package id.walt.credentials.w3c

import id.walt.credentials.selectiveDisclosure.SDField
import id.walt.credentials.w3c.builder.AbstractW3CCredentialBuilder
import id.walt.credentials.w3c.builder.CredentialFactory
import id.walt.services.sdjwt.SDJwtService
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
                this.sdJwt = verifiableCredential.sdJwt
            }

        fun fromString(data: String) = fromVerifiableCredential(data.toVerifiableCredential())
    }
}

class VerifiablePresentationBuilder : AbstractW3CCredentialBuilder<VerifiablePresentation, VerifiablePresentationBuilder>(
    listOf("VerifiablePresentation"),
    VerifiablePresentation
) {
    fun setHolder(holder: String) = setProperty("holder", holder)

    fun setVerifiableCredentials(verifiableCredentials: List<PresentableCredential>) =
        setProperty("verifiableCredential", verifiableCredentials.map { it.toJsonElement() }.toList())
}

fun String.toVerifiablePresentation() = VerifiablePresentation.fromString(this)

data class PresentableCredential(
    val verifiableCredential: VerifiableCredential,
    val selectiveDisclosure: Map<String, SDField>? = null,
    val discloseAll: Boolean = false
) {
    fun toJsonElement() =
        if(verifiableCredential.sdJwt != null) {
            val claimKey = VerifiableCredential.possibleClaimKeys.first { it in verifiableCredential.sdJwt!!.sdPayload.keys }
            val presentedJwt = if(discloseAll) {
                SDJwtService.getService().present(verifiableCredential.sdJwt!!, discloseAll)
            } else {
                SDJwtService.getService().present(verifiableCredential.sdJwt!!, selectiveDisclosure?.let { mapOf(
                    claimKey to SDField(true, it)
                )})
            }
            JsonPrimitive(presentedJwt.toString())
        } else verifiableCredential.toJsonElement()

    val isJwt
        get() = verifiableCredential.sdJwt != null
}
