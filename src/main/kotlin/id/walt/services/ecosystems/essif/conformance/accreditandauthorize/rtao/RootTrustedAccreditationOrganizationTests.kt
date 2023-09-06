package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.rtao

import id.walt.services.ecosystems.essif.conformance.Test

object RootTrustedAccreditationOrganizationTests : Test {
    override fun run() {
        RequestVerifiableAuthorisationForTrustChain.run()
        RegisterVerifiableAuthorisationForTrustChainToTIR.run()
    }
}
