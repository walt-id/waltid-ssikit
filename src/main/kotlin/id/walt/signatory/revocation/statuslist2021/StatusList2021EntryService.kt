package id.walt.signatory.revocation.statuslist2021

import com.beust.klaxon.Json
import id.walt.common.createEncodedBitString
import id.walt.common.resolveContent
import id.walt.common.toBitSet
import id.walt.common.uncompressGzip
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import id.walt.crypto.decBase64
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.signatory.revocation.RevocationConfig
import id.walt.signatory.revocation.StatusListRevocationConfig
import kotlinx.serialization.Serializable
import java.util.*

object StatusList2021EntryService {

    private val credentialStorage = StatusListCredentialStorageService.getService()

    fun checkRevoked(credentialStatus: StatusList2021EntryCredentialStatus): Boolean = let {
        val credentialSubject = extractStatusListCredentialSubject(credentialStatus.statusListCredential) ?: throw IllegalArgumentException("Couldn't parse credential subject")
        val credentialIndex = credentialStatus.statusListIndex.toULongOrNull()?: throw IllegalArgumentException("Couldn't parse status list index")
        if(!verifyStatusPurpose(credentialStatus.statusPurpose, credentialSubject.statusPurpose)) throw IllegalArgumentException("Status purposes don't match")
        verifyStatusCredential()

        verifyBitStringStatus(credentialIndex, credentialSubject.encodedList)
    }

    fun setRevocation(config: RevocationConfig) {
        val configParam = config as StatusListRevocationConfig
        // credential
        val credential = resolveContent(configParam.statusEntry.statusListCredential).toVerifiableCredential()
        // check if credential with id exists
        val statusCredential = credentialStorage.fetch(credential.id ?: "")
            ?: throw IllegalArgumentException("No status credential found for the provided id: ${credential.id}")
        updateStatusCredentialRecord(statusCredential, configParam.statusEntry.statusListIndex)
    }

    private fun updateStatusCredentialRecord(statusCredential: VerifiableCredential, index: String){
        val credentialSubject = extractStatusListCredentialSubject(statusCredential)!!
        // get credential index
        val idx = index.toIntOrNull()?: throw IllegalArgumentException("Couldn't parse credential index")
        // get bitString
        val bitString = uncompressGzip(Base64.getDecoder().decode(credentialSubject.encodedList))
        val bitSet = bitString.toBitSet(16 * 1024 * 8)
        // set the respective bit
        bitSet.set(idx)
        val encodedList = createEncodedBitString(bitSet)
        // create / update the status list credential
        credentialStorage.store(statusCredential.id!!, credentialSubject.statusPurpose, String(encodedList))
    }

    private fun extractStatusListCredentialSubject(statusCredential: String): StatusListCredentialSubject? =
        extractStatusListCredentialSubject(resolveContent(statusCredential).toVerifiableCredential())

    private fun extractStatusListCredentialSubject(statusCredential: VerifiableCredential) =
        statusCredential.credentialSubject?.let {
            StatusListCredentialSubject(
                id = it.id,
                type = it.properties["type"] as? String ?: "",
                statusPurpose = it.properties["statusPurpose"] as? String ?: "",
                encodedList = it.properties["encodedList"] as? String ?: "",
            )
        }

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
