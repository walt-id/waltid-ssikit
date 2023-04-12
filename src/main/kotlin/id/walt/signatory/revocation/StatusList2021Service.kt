package id.walt.signatory.revocation

import com.beust.klaxon.Json
import id.walt.common.uncompressGzip
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.decBase64
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.services.WaltIdServices
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

object StatusList2021Service {

    private val httpClient = WaltIdServices.httpNoAuth

    fun checkRevoked(credentialStatus: StatusList2021EntryCredentialStatus): Boolean = let {
        val statusListCredential = runBlocking { httpClient.get(credentialStatus.statusListCredential).bodyAsText() }.toVerifiableCredential()
        val credentialSubject = extractStatusListCredentialSubject(statusListCredential) ?: throw IllegalArgumentException("Couldn't parse credential subject")
        val credentialIndex = credentialStatus.statusListIndex.toULongOrNull()?: throw IllegalArgumentException("Couldn't parse status list index")
        if(!verifyStatusPurpose(credentialStatus.statusPurpose, credentialSubject.statusPurpose)) throw IllegalArgumentException("Status purposes don't match")

        verifyBitStringStatus(credentialIndex, credentialSubject.encodedList)
    }

    private fun extractStatusListCredentialSubject(statusCredential: VerifiableCredential) =
        statusCredential.credentialSubject?.let {
            StatusListCredentialSubject(
                id = it.id,
                type = it.properties["type"] as? String ?: "",
                statusPurpose = it.properties["statusPurpose"] as? String ?: "",
                encodedList = it.properties["encodedList"] as? String ?: "",
            )
        }

    private fun verifyProofs() = true

    private fun verifyStatusPurpose(entryPurpose: String, credentialPurpose: String) =
        entryPurpose.equals(credentialPurpose, ignoreCase = true)

    private fun verifyBitStringStatus(idx: ULong, encodedList: String) = uncompressGzip(decBase64(encodedList), idx)[0] == '1'

    @Serializable
    data class StatusListCredentialSubject(
        @Json(serializeNull = false)
        val id: String? = null,
        val type: String,
        val statusPurpose: String,
        val encodedList: String,
    )
}
