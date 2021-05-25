package org.letstrust.vclib.vcs


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.vclib.VCMetadata

@Serializable
data class EbsiVerifiableAttestation(
    @SerialName("@context")
    override val context: List<String>,
    var id: String?,
    override val type: List<String>,
    var issuer: String?,
    var issuanceDate: String?, // 2020-04-22T10:37:22Z
    val credentialSubject: CredentialSubject?,
    val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    val proof: Proof? = null
) : VC {
    @Serializable
    data class CredentialSubject(
        var id: String?,
        val authorizationClaims: List<String>?
    )

    @Serializable
    data class CredentialStatus(
        var id: String?,
        var type: String?
    )

    @Serializable
    data class CredentialSchema(
        var id: String?,
        var type: String?
    )

    companion object : VCMetadata {
        override val metadataContext = ""
        override val metadataType = "VerifiableAttestation"
    }
}

fun EbsiVerifiableAttestation.encode() = Json.encodeToString(this)
fun EbsiVerifiableAttestation.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
