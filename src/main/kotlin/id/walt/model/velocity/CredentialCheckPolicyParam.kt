package id.walt.model.velocity

import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue

data class CredentialCheckPolicyParam(
    val checkList: Map<CredentialCheckType, CredentialCheckValue>
)
