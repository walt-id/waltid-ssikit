package id.walt.signatory.revocation

import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.SimpleCredentialStatus2022
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
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
    private val indexService: StatusListIndexService
) : CredentialStatusFactory {
    override fun create(parameter: CredentialStatusFactoryParameter) = let {
        indexService.read() ?: indexService.create()
    }.let {
        val statusParameter = parameter as StatusListEntryFactoryParameter
        // update index
        indexService.update(StatusListIndex(
            index = ((it.index.toIntOrNull() ?: 0) + 1).toString()
        ))
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
    val purpose: String
) : CredentialStatusFactoryParameter
