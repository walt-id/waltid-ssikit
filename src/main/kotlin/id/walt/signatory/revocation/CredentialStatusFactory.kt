package id.walt.signatory.revocation

import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.SimpleCredentialStatus2022
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.signatory.revocation.statuslist2021.StatusListCredentialStorageService
import id.walt.signatory.revocation.statuslist2021.StatusListIndex
import id.walt.signatory.revocation.statuslist2021.StatusListIndexService

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
    private val storageService: StatusListCredentialStorageService
) : CredentialStatusFactory {
    override fun create(parameter: CredentialStatusFactoryParameter) = let {
        indexService.read() ?: indexService.create()
    }.let {
        // update index
        indexService.update(StatusListIndex(
            index = ((it.index.toIntOrNull() ?: 0) + 1).toString()
        ))
        StatusList2021EntryCredentialStatus(
            id = storageService.publicUrl + it.index,
            statusPurpose = (parameter as StatusListEntryFactoryParameter).purpose,
            statusListIndex = it.index,
            statusListCredential = storageService.publicUrl,
        )
    }
}

interface CredentialStatusFactoryParameter
data class SimpleStatusFactoryParameter(
    val id: String,
) : CredentialStatusFactoryParameter
data class StatusListEntryFactoryParameter(
    val purpose: String
) : CredentialStatusFactoryParameter
