package id.walt.services.essif

import com.beust.klaxon.Json
import id.walt.vclib.model.CredentialSchema
import id.walt.vclib.model.Proof
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//@Serializable
data class EbsiVAWrapper(
    val verifiableCredential: EbsiVa,
    @Json(serializeNull = false)
    val proof: Proof? = null
)

data class EbsiVa(
    @Json(name = "@context")
    @SerialName("@context")
    val context: List<String>,
    var id: String?,
    val type: List<String>,
    var issuer: String?,
    val validFrom: String?,// 2021-05-31T12:29:47Z
    var issuanceDate: String?, // 2020-04-22T10:37:22Z
    @Json(serializeNull = false) var expirationDate: String? = null, // 2022-04-22T10:37:22Z
    val credentialSubject: CredentialSubject?,
    // val credentialStatus: CredentialStatus?,
    val credentialSchema: CredentialSchema?,
    @Json(serializeNull = false) val proof: Proof? = null
) {


    @Serializable
    data class CredentialSubject(
        var id: String?
    )

    /*@Serializable
    data class CredentialStatus(
        var id: String?,
        var type: String?
    )*/

    @Serializable
    data class CredentialSchema(
        var id: String?,
        var type: String?
    )

    /*
    @Serializable
    data class Proof(
        @Json(serializeNull = false) val type: String? = null, // "Ed25519Signature2018"
        @Json(serializeNull = false) val creator: String? = null, //did:ebsi:2sMvVBpwueU8j6WBnJcUAkcNnPXLQvGy3a6a3X59wKRnJzZQ
        @Json(serializeNull = false) val created: String? = null, //"2020-04-22T10:37:22Z",
        @Json(serializeNull = false) val proofPurpose: String? = null, //"assertionMethod",
        @Json(serializeNull = false) val verificationMethod: String? = null, //"did:example:456#key-1",
        @Json(serializeNull = false) val jws: String? = null, //"eyJjcml0IjpbImI2NCJdLCJiNjQiOmZhbHNlLCJhbGciOiJFZERTQSJ9..BhWew0x-txcroGjgdtK-yBCqoetg9DD9SgV4245TmXJi-PmqFzux6Cwaph0r-mbqzlE17yLebjfqbRT275U1AA"
    )*/


}

//fun EbsiVa.encode() = Klaxon().toJsonString(this)
//fun EbsiVa.encodePretty() = Klaxon().toJsonString(this)


data class EbsiVaVp(
    @Json(name = "@context")
    @SerialName("@context")
    val context: List<String>,
    val type: List<String>,
    @Json(serializeNull = false) val id: String? = null,
    @Json(serializeNull = false) val verifiableCredential: List<EbsiVa>?,
    val holder: String,  // "holder": "did:ebsi:yMe3d2JDquN27rMPqoYjFVZhs8BcwVWbpgY1qHZy8zG",
    @Json(serializeNull = false) val proof: Proof? = null
) // TODO replace with : VerifiablePresentation()

//fun EbsiVaVp.encode() = Klaxon().toJsonString(this)
//fun EbsiVaVp.encodePretty() = Klaxon().toJsonString(this)
