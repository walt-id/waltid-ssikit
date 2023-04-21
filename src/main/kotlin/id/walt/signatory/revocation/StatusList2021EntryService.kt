package id.walt.signatory.revocation

import com.beust.klaxon.Json
import id.walt.common.createEncodedBitString
import id.walt.common.resolveContent
import id.walt.common.toBitSet
import id.walt.common.uncompressGzip
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.decBase64
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.signatory.revocation.repository.StatusListRevocationRepository
import kotlinx.serialization.Serializable
import java.util.*

object StatusList2021EntryService {

    private val repository = StatusListRevocationRepository()
    fun checkRevoked(credentialStatus: StatusList2021EntryCredentialStatus): Boolean = let {
        val credentialSubject = extractStatusListCredentialSubject(credentialStatus.statusListCredential) ?: throw IllegalArgumentException("Couldn't parse credential subject")
        val credentialIndex = credentialStatus.statusListIndex.toULongOrNull()?: throw IllegalArgumentException("Couldn't parse status list index")
        if(!verifyStatusPurpose(credentialStatus.statusPurpose, credentialSubject.statusPurpose)) throw IllegalArgumentException("Status purposes don't match")
        verifyStatusCredential()

        verifyBitStringStatus(credentialIndex, credentialSubject.encodedList)
    }

    fun setRevocation(config: RevocationConfig) {
        val configParam = config as StatusListRevocationConfig
        // get status credential subject
        val credentialSubject =
            extractStatusListCredentialSubject(configParam.status.statusListCredential)
                ?: throw IllegalArgumentException("Couldn't parse credential subject")
        // get bitString
        val bitString = uncompressGzip(Base64.getDecoder().decode(credentialSubject.encodedList))
        val bitSet = bitString.toBitSet(16 * 1024 * 8)
        // get credential index
        val idx = configParam.status.statusListIndex.toIntOrNull()?: throw IllegalArgumentException("Couldn't parse credential index")
        // set the respective bit
        bitSet.set(idx)
        val encodedList = createEncodedBitString(bitSet)
        // generate status-list credential and store it
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

    private fun extractStatusListCredentialSubject(statusCredential: String) =
        extractStatusListCredentialSubject(resolveContent(statusCredential).toVerifiableCredential())

    /* TODO:
    - proofs
    - matching issuers
     */
    private fun verifyStatusCredential() = true

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
