package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.rtao

import id.walt.services.ecosystems.essif.conformance.Test

object RootTrustedAccreditationOrganizationTests : Test {
    override suspend fun run() {
        RequestVerifiableAuthorisationForTrustChain.run()
        RegisterVerifiableAuthorisationForTrustChainToTIR.run()
    }
}
