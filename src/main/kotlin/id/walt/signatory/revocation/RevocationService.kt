package id.walt.signatory.revocation

import com.beust.klaxon.Json
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import kotlinx.serialization.Serializable

interface RevocationService {
    fun checkRevocation(parameter: RevocationParameter): RevocationResult
    fun getRevocation(): RevocationData
    fun clearAll()
    fun setRevocation(parameter: RevocationParameter)
}

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
    @Json(serializeNull = false) val timeOfRevocation: Long? = null,
    override val isRevoked: Boolean
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
