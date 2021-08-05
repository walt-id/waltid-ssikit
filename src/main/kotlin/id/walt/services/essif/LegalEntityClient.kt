package id.walt.services.essif

import id.walt.services.essif.mock.RelyingParty


/**
 * The LegalEntityClient simulates a remote Legal Entity, Relying Party, Trusted Issuer or eSSIF onboarding service.
 */
object LegalEntityClient {
    var le = TrustedIssuerClient
    var rp = RelyingParty
    var ti = TrustedIssuerClient
    var eos = TrustedIssuerClient
}
