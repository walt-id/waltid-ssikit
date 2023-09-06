package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.ti

import id.walt.services.ecosystems.essif.conformance.Test

object TrustedIssuerTests : Test {
    override fun run() {
        Onboarding.run()
        AccreditationAsTrustedIssuer.run()
        IssuerAndRevoke.run()
    }
}
