package org.letstrust.crypto

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import org.letstrust.services.key.KeyManagementService
import java.security.InvalidKeyException

object CryptoService {
    var ecJWK: ECKey? = null

    fun generate(): String {
        ecJWK = ECKeyGenerator(Curve.SECP256K1)
            .keyUse(KeyUse.SIGNATURE)
            .keyID("123")
            .generate()
        return ecJWK!!.keyID
    }

    fun sign(keyId: String, data: ByteArray): ByteArray {

        val signingKey = KeyManagementService.loadKeys(keyId)

        val jcaSignature = try {
            val dsa = ECDSA.getSignerAndVerifier(JWSAlgorithm.ES256K, null)
            dsa.initSign(signingKey!!.toEcKey().toECPrivateKey())
            dsa.update(data)
            dsa.sign()
        } catch (e: InvalidKeyException) {
            throw JOSEException(e.message, e)
        }
        return jcaSignature
    }

    fun verify(keyId: String, signature: ByteArray) {

    }
}
