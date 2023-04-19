package id.walt.signatory.revocation

import com.beust.klaxon.Json
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import kotlinx.serialization.Serializable

interface RevocationService {
    fun checkRevocation(parameter: RevocationParameter): RevocationResult
    fun getRevocation(): RevocationData
    fun clearAll()
    fun setRevocation(parameter: RevocationConfig)
}

data class RevocationList(val revokedList: List<RevocationResult>)

/*
Revocation results
 */
@Serializable
abstract class RevocationResult {
    abstract val isRevoked: Boolean
}

@Serializable
data class TokenRevocationResult(
    val token: String,
    override val isRevoked: Boolean,
    @Json(serializeNull = false)
    val timeOfRevocation: Long? = null
) : RevocationResult()

@Serializable
data class StatusListRevocationResult(
    override val isRevoked: Boolean
) : RevocationResult()

/*
Revocation parameters
 */
interface RevocationParameter
data class TokenRevocationParameter(
    val token: String,
) : RevocationParameter

data class StatusListRevocationParameter(
    val credentialStatus: StatusList2021EntryCredentialStatus,
) : RevocationParameter

/*
Revocation data
 */
interface RevocationData

/*
Revocation config
 */
interface RevocationConfig

@Serializable
data class TokenRevocationConfig(
    val token: String,
) : RevocationConfig

@Serializable
data class StatusListRevocationConfig(
    val id: String,
) : RevocationConfig
