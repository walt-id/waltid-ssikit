package id.walt.model.gaiax

import id.walt.credentials.w3c.VerifiableCredential

data class GaiaxCredentialGroup(
    val complianceCredential: VerifiableCredential,
    val selfDescriptionCredential: VerifiableCredential // todo: interface this
)
