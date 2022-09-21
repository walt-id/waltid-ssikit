package id.walt.model.gaiax

import id.walt.vclib.credentials.gaiax.n.LegalPerson
import id.walt.vclib.credentials.gaiax.n.ParticipantCredential

data class GaiaxCredentialGroup(
    val complianceCredential: ParticipantCredential,
    val selfDescriptionCredential: LegalPerson // todo: interface this
)
