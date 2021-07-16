package org.letstrust.services.essif

import org.letstrust.services.essif.mock.RelyingParty


/**
 * The LegalEntityClient simulates a remote Legal Entity, Relying Party, Trusted Issuer or eSSIF onboarding service.
 */
object LegalEntityClient {
    var le = EosService
    var rp = RelyingParty
    var ti = EosService
    var eos = EosService
}
