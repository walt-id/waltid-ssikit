package org.letstrust.services.essif

import kotlinx.serialization.Serializable
import org.letstrust.services.essif.mock.RelyingParty
import java.util.*

@Serializable
data class AuthRequestResponse(val session_token: String)

@Serializable
data class DidAuthRequest(val reponse_type: String, val client_id: String, val scope: String, val nonce: String, val request: String)

data class DidAuthRequestJwt(val scope: String, val iss: String, val response_type: String, val exp: Date, val iat: Date, val nonce: String, val client_id: String)


@Serializable
data class DecryptedAccessTokenResponse(
    val access_token: String,
    val did: String,
    val nonce: String
)

/**
 * The LegalEntityClient simulates a remote Leagal Entity, Relying Party, Trusted Issuer or eSSIF onboarding service.
 */
object LegalEntityClient {
    val le = EosService
    val rp = RelyingParty
    val ti = EosService
    val eos = EosService
}
