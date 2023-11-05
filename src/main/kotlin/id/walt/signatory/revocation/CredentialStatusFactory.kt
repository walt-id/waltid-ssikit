package id.walt.signatory.revocation

import id.walt.common.createEncodedBitString
import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.SimpleCredentialStatus2022
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.signatory.revocation.statuslist2021.StatusListCredentialStorageService
import id.walt.signatory.revocation.statuslist2021.StatusListIndex
import id.walt.signatory.revocation.statuslist2021.StatusListIndexService
import java.util.*

interface CredentialStatusFactory {
    fun create(parameter: CredentialStatusFactoryParameter): CredentialStatus
}

class SimpleCredentialStatusFactory : CredentialStatusFactory {
    override fun create(parameter: CredentialStatusFactoryParameter) = SimpleCredentialStatus2022(
        id = (parameter as? SimpleStatusFactoryParameter)?.id ?: ""
    )
}

class StatusListEntryFactory(
    private val indexService: StatusListIndexService,
    private val storageService: StatusListCredentialStorageService,
) : CredentialStatusFactory {
    override fun create(parameter: CredentialStatusFactoryParameter) = let {
        indexService.read() ?: indexService.create()
    }.let {
        val statusParameter = parameter as StatusListEntryFactoryParameter
        // update index
        indexService.update(StatusListIndex(
            index = ((it.index.toIntOrNull() ?: 0) + 1).toString()
        ))
        // verify status-credential exists and create one
        storageService.fetch(statusParameter.credentialUrl) ?: run {
            storageService.store(
                parameter.issuer,
                statusParameter.credentialUrl,
                statusParameter.purpose,
                String(createEncodedBitString(BitSet(16 * 1024 * 8)))
            )
        }
        StatusList2021EntryCredentialStatus(
            id = statusParameter.credentialUrl + "#${it.index}",
            statusPurpose = statusParameter.purpose,
            statusListIndex = it.index,
            statusListCredential = statusParameter.credentialUrl,
        )
    }
}

interface CredentialStatusFactoryParameter
data class SimpleStatusFactoryParameter(
    val id: String,
) : CredentialStatusFactoryParameter
data class StatusListEntryFactoryParameter(
    val credentialUrl: String,
    val purpose: String,
    val issuer: String,
) : CredentialStatusFactoryParameter
