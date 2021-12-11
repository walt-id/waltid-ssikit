package id.walt.rest.custodian

import com.beust.klaxon.Json
import kotlinx.serialization.Serializable

@Serializable
data class PresentCredentialsRequest(
    val vcs: List<String>,
    val holderDid: String,
    @Json(serializeNull = false) val verifierDid: String? = null,
    @Json(serializeNull = false) val domain: String? = null,
    @Json(serializeNull = false) val challenge: String? = null
)

@Serializable
data class PresentCredentialIdsRequest(
    val vcIds: List<String>,
    val holderDid: String,
    @Json(serializeNull = false) val verifierDid: String? = null,
    @Json(serializeNull = false) val domain: String? = null,
    @Json(serializeNull = false) val challenge: String? = null
)
