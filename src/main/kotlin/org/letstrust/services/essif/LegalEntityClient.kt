package org.letstrust.services.essif

import kotlinx.serialization.Serializable
import org.letstrust.model.AuthenticationRequestHeader
import org.letstrust.services.essif.mock.RelyingParty
import java.util.*



/**
 * The LegalEntityClient simulates a remote Legal Entity, Relying Party, Trusted Issuer or eSSIF onboarding service.
 */
object LegalEntityClient {
    val le = EosService
    val rp = RelyingParty
    val ti = EosService
    val eos = EosService
}
