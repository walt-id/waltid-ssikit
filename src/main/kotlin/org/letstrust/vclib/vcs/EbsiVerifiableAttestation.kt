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

    @Serializable
    data class Proof(
        val type: String?, // EcdsaSecp256k1Signature2019
        val created: String?, // 2019-06-22T14:11:44Z
        val creator: String? = null,
        val domain: String? = null,
        val nonce: String? = null,
        val jws: String?, // eyJhbGciOiJSUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..TCYt5X
    )

    companion object : VCMetadata {
        override val metadataContext = ""
        override val metadataType = "VerifiableAttestation"
    }
}
