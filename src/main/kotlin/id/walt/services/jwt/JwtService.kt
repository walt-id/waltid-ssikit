package id.walt.services.jwt

import com.nimbusds.jose.jwk.OctetKeyPair
import id.walt.sdjwt.JWTCryptoProvider
import id.walt.sdjwt.JwtVerificationResult
import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import kotlinx.serialization.json.JsonObject

open class JwtService : WaltIdService(), JWTCryptoProvider {

    override val implementation get() = serviceImplementation<JwtService>()

    // TODO load key and load encryption config based on key-alg
    // val recipientJWK = KeyManagementService.loadKeys(keyAlias)
    // if (recipientJWK == null) {
    //     log.error { "Could not load verifying key for $keyAlias" }
    //     throw NoSuchElementException("Could not load verifying key for $keyAlias")
    // }

    open fun encrypt(
        kid: String,
        pubEncKey: OctetKeyPair,
        payload: String? = null
    ): String = implementation.encrypt(kid, pubEncKey, payload)

    open fun sign(
        keyAlias: String, // verification method
        payload: String? = null
    ): String = implementation.sign(keyAlias, payload)

    override fun sign(payload: JsonObject, keyID: String?): String {
        if(keyID == null) {
            throw Exception("KeyID not provided")
        }
        return sign(keyID, payload.toString())
    }

    override fun verify(token: String): JwtVerificationResult = implementation.verify(token)

    open fun parseClaims(token: String): MutableMap<String, Any>? = implementation.parseClaims(token)

    companion object : ServiceProvider {
        override fun getService() = object : JwtService() {}
        override fun defaultImplementation() = WaltIdJwtService()
    }
}
