package id.walt.services.ecosystems.essif

import com.beust.klaxon.Json
import id.walt.common.SingleVC
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.W3CProof

//@Serializable
data class EbsiVAWrapper(
    @SingleVC val verifiableCredential: VerifiableCredential,
    @Json(serializeNull = false)
    val proof: W3CProof? = null
)
