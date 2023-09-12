package id.walt.services.ecosystems.essif.conformance.accreditandauthorize

import id.walt.services.ecosystems.essif.conformance.Test
import id.walt.services.ecosystems.essif.conformance.accreditandauthorize.rtao.RootTrustedAccreditationOrganizationTests
import id.walt.services.ecosystems.essif.conformance.accreditandauthorize.tao.TrustedAccreditationOrganizationTests
import id.walt.services.ecosystems.essif.conformance.accreditandauthorize.ti.TrustedIssuerTests

object AccreditAndAuthorizeTests : Test {
    override suspend fun run() {
        TrustedIssuerTests.run()
        TrustedAccreditationOrganizationTests.run()
        RootTrustedAccreditationOrganizationTests.run()
    }
}
