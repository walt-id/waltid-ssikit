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
    var expirationDate: String?, // 2022-04-22T10:37:22Z
    val credentialSubject: CredentialSubject?,
    val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    val proof: Proof? = null
) : VC {

    override fun issuer(): String = issuer!!

    override fun holder(): String = credentialSubject!!.id!!

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

@Serializable
data class EbsiVerifiableAttestationVP(
    @SerialName("@context")
    override val context: List<String>,
    override val type: List<String>,
    val id: String? = null,
    val vc: List<EbsiVerifiableAttestation>?,
    val proof: org.letstrust.model.Proof? = null
) : VC {

    override fun issuer(): String = proof!!.creator!!

    override fun holder(): String = proof!!.creator!!

    companion object : VCMetadata {
        override val metadataContext = ""
        override val metadataType = "VerifiablePresentation"
    }
}

fun EbsiVerifiableAttestationVP.encode() = Json.encodeToString(this)
fun EbsiVerifiableAttestationVP.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
