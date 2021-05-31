package org.letstrust.vclib.vcs


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.vclib.VCMetadata


@Serializable
data class EbsiVerifiableAuthorisation(
    @SerialName("@context")
    override val context: List<String>,
    var id: String?,
    override val type: List<String>,
    var issuer: String?,
    val validFrom: String?,// 2021-05-31T12:29:47Z
    var issuanceDate: String?, // 2020-04-22T10:37:22Z
    var expirationDate: String? = null, // 2022-04-22T10:37:22Z
    val credentialSubject: CredentialSubject?,
    // val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    val proof: Proof? = null
) : VC {

    override fun issuer(): String = issuer!!

    override fun holder(): String = credentialSubject!!.id!!

    @Serializable
    data class CredentialSubject(
        var id: String?
    )

//    @Serializable
//    data class CredentialStatus(
//        var id: String?,
//        var type: String?
//    )

    @Serializable
    data class CredentialSchema(
        var id: String?,
        var type: String?
    )

    companion object : VCMetadata {
        override val metadataContext = "https://w3c-ccg.github.io/lds-jws2020/contexts/lds-jws2020-v1.json"
        override val metadataType = "VerifiableAuthorisation"
    }
}

fun EbsiVerifiableAuthorisation.encode() = Json.encodeToString(this)
fun EbsiVerifiableAuthorisation.encodePretty() = Json { prettyPrint = true }.encodeToString(this)

@Serializable
data class EbsiVerifiableAuthorisationVP(
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

fun EbsiVerifiableAuthorisationVP.encode() = Json.encodeToString(this)
fun EbsiVerifiableAuthorisationVP.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
