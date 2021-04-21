package org.letstrust.services.essif

import org.letstrust.services.essif.mock.RelyingParty

/**
 * The LegalEntityClient simulates a remote Leagal Entity, Relying Party, Trusted Issuer or eSSIF onboarding service.
 */
object LegalEntityClient {
    val le = EosService
    val rp = RelyingParty
    val ti = EosService
    val eos = EosService
}
