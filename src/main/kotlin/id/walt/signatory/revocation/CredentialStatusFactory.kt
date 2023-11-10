package id.walt.signatory.revocation

import id.walt.model.credential.status.CredentialStatus
import id.walt.signatory.revocation.simplestatus2022.SimpleCredentialClientService
import id.walt.signatory.revocation.statuslist2021.StatusList2021EntryClientService

object CredentialStatusFactory {
    private val simpleStatus = SimpleCredentialClientService()
    private val statusList2021 = StatusList2021EntryClientService()
    fun create(parameter: CredentialStatusFactoryParameter): CredentialStatus = when (parameter) {
        is SimpleStatusFactoryParameter -> simpleStatus.create(parameter)
        is StatusListEntryFactoryParameter -> statusList2021.create(parameter)
        else -> throw IllegalArgumentException("Status type not supported: ${parameter.javaClass.simpleName}")
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
