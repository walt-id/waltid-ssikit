package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.tao

import id.walt.services.ecosystems.essif.conformance.CredentialIssuanceFlow
import id.walt.services.ecosystems.essif.conformance.Test

object RequestVerifiableAccreditationToAccredit : Test {
    override fun run() {
        val credential = CredentialIssuanceFlow.getCredential("VerifiableAccreditationToAccredit")
        TODO("Not yet implemented")
    }
}
