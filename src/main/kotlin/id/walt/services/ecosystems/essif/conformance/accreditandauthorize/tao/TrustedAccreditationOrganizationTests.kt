package id.walt.services.ecosystems.essif.conformance.accreditandauthorize.tao

import id.walt.services.ecosystems.essif.conformance.Test

object TrustedAccreditationOrganizationTests : Test {
    override fun run() {
        RequestVerifiableAccreditationToAccredit.run()
        RegisterVerifiableAccreditationToAccreditToTIR.run()
        IssueVerifiableAuthorisationToOnboardForSubAccount.run()
        IssueVerifiableAccreditationToAttestForSubAccount.run()
        IssueVerifiableAccreditationToAccreditForSubAccount.run()
        RevokeAccreditationsForSubAccount.run()
    }
}
