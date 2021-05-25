package org.letstrust.vclib.vcs


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.letstrust.vclib.VCMetadata

@Serializable
data class EbsiVerifiableAttestation(
    @SerialName("@context")
    override val context: List<String>,
    val id: String?,
    override val type: List<String>,
    var issuer: String?,
    val issuanceDate: String?, // 2020-04-22T10:37:22Z
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
        val id: String?,
        val type: String?
    )
    @Serializable
    data class CredentialSchema(
        val id: String?,
        val type: String?
    )

    companion object : VCMetadata {
        override val metadataContext = ""
        override val metadataType = "VerifiableAttestation"
    }
}
