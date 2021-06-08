package org.letstrust.services.essif

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.letstrust.vclib.vcs.Proof

@Serializable
data class EbsiVAWrapper(val verifiableCredential: EbsiVa, val proof: Proof? = null)


@Serializable
data class EbsiVa(
    @SerialName("@context")
    val context: List<String>,
    var id: String?,
    val type: List<String>,
    var issuer: String?,
    val validFrom: String?,// 2021-05-31T12:29:47Z
    var issuanceDate: String?, // 2020-04-22T10:37:22Z
    var expirationDate: String? = null, // 2022-04-22T10:37:22Z
    val credentialSubject: CredentialSubject?,
    // val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    val proof: Proof? = null
) {


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

}

fun EbsiVa.encode() = Json.encodeToString(this)
fun EbsiVa.encodePretty() = Json { prettyPrint = true }.encodeToString(this)

@Serializable
data class EbsiVaVp(
    @SerialName("@context")
    val context: List<String>,
    val type: List<String>,
    val id: String? = null,
    val verifiableCredential: List<EbsiVa>?,
    val proof: Proof? = null
)

fun EbsiVaVp.encode() = Json.encodeToString(this)
fun EbsiVaVp.encodePretty() = Json { prettyPrint = true }.encodeToString(this)
