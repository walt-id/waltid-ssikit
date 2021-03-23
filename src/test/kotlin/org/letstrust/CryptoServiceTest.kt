package org.letstrust

import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.impl.AlgorithmSupportMessage
import com.nimbusds.jose.crypto.impl.ECDSA
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.Test
import org.letstrust.crypto.LetsTrustProvider
import java.security.*




// SignatureSpi()



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

        val jcaSignature = try {
            val dsa = Signature.getInstance("ES256K");// ECDSA.getSignerAndVerifier(JWSAlgorithm.ES256K, null)
            dsa.initSign(ecJWK!!.toPrivateKey())
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

// Proxy for enabling secure key storage
class LtECDSASigner(val keyId: String) : JWSSigner {

    val nimbusSigner = com.nimbusds.jose.crypto.ECDSASigner(CryptoService.ecJWK)


    override fun getJCAContext(): JCAContext {
        return nimbusSigner.jcaContext
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
        return nimbusSigner.supportedJWSAlgorithms()
    }

    override fun sign(header: JWSHeader?, signingInput: ByteArray?): Base64URL {
        // return nimbusSigner.sign(header, signingInput)

        val alg = header!!.algorithm

        if (!supportedJWSAlgorithms().contains(alg)) {
            throw JOSEException(AlgorithmSupportMessage.unsupportedJWSAlgorithm(alg, supportedJWSAlgorithms()))
        }

        // DER-encoded signature, according to JCA spec
        // (sequence of two integers - R + S)

        val jcaSignature = CryptoService.sign(keyId, signingInput!!)

        // OR keyId to PrivateKey Handle + JCA Provider

        //       val jcaSignature: ByteArray

//        jcaSignature = try {
//            val dsa = ECDSA.getSignerAndVerifier(alg, jcaContext.provider)
//            dsa.initSign(privateKey, jcaContext.secureRandom)
//            dsa.update(signingInput)
//            dsa.sign()
//        } catch (e: InvalidKeyException) {
//            throw JOSEException(e.message, e)
//        } catch (e: SignatureException) {
//            throw JOSEException(e.message, e)
//        }

        val rsByteArrayLength = ECDSA.getSignatureByteArrayLength(header!!.algorithm)
        val jwsSignature = ECDSA.transcodeSignatureToConcat(jcaSignature, rsByteArrayLength)
        return Base64URL.encode(jwsSignature)

    }
    // ECDSASigner(ecJWK)
}

class JwtVerifier() : JWSVerifier {
    override fun getJCAContext(): JCAContext {
        TODO("Not yet implemented")
    }

    override fun supportedJWSAlgorithms(): MutableSet<JWSAlgorithm> {
        TODO("Not yet implemented")
    }

    override fun verify(header: JWSHeader?, signingInput: ByteArray?, signature: Base64URL?): Boolean {
        TODO("Not yet implemented")
    }


}

class CryptoServiceTest {

    @Test
    fun testJwtSigner() {

        val keyId = CryptoService.generate()

        // Get the public EC key, for recipients to validate the signatures
        //val ecPublicJWK = CryptoService.ecJWK?.toPublicJWK()

        // Sample JWT claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .build()

        // Create JWT for ES256K alg
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K)
                .keyID(keyId)
                .build(),
            claimsSet
        )

        // Sign with private EC key


        jwt.sign(LtECDSASigner(keyId))
//
//        // Output the JWT
        println(jwt.serialize())
    }

    @Test
    fun testProviderSign() {

      //  Security.addProvider(LetsTrustProvider());


        val ecJWK = ECKeyGenerator(Curve.SECP256K1)
            .keyUse(KeyUse.SIGNATURE)
            .keyID("123")
            .generate()

        // Get the public EC key, for recipients to validate the signatures

        val ecPublicJWK = ecJWK.toPublicJWK()

        // Sample JWT claims
        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .build()

        // Create JWT for ES256K alg
        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K)
                .keyID(ecJWK.keyID)
                .build(),
            claimsSet
        )

        val signer = ECDSASigner(ecJWK)
        signer.jcaContext.provider = LetsTrustProvider()
        val sig = jwt.sign(signer)
        println(jwt.serialize())

    }
    @Test
    @Throws(Exception::class)
    fun testProviders() {

        Security.addProvider(LetsTrustProvider());

        val mysig = Signature.getInstance("ES256k", "LT")
        println(mysig)

        val myks = KeyStore.getInstance("PKCS11", "LT")
        println(myks)

        val providers: Array<Provider> = Security.getProviders()

        for (p in providers) {
            val info: String = p.getInfo()
            println(p.toString() + " - " + info)

            p.services.forEach { s -> println(s) }
        }
    }


//    fun hsm() {
//        val configFile = "hsm-config.cfg"
//
//        // Load the HSM as a Java crypto provider
//        val hsmProvider: Provider = SunPKCS11()
//
//
//        // Get a handle to the private RSA key for signing
//        val hsmKeyStore = KeyStore.getInstance("PKCS11", hsmProvider)
//        val userPin = "123456" // The pin to unlock the HSM
//
//        hsmKeyStore.load(null, userPin.toCharArray())
//        val keyID = "1" // The key identifier or alias
//
//        val keyPin = "" // Optional pin to unlock the key
//
//        val privateKey = hsmKeyStore.getKey(keyID, keyPin.toCharArray()) as PrivateKey
//
//        val sig = MySig("PhilSig")
//        // Create an RSA signer and configure it to use the HSM
//        val signer = RSASSASigner(privateKey)
//        signer.jcaContext.provider = hsmProvider
//
//        // We can now RSA sign JWTs
//        val jwt = SignedJWT(
//            JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyID).build(),
//            JWTClaimsSet.Builder().subject("alice").build()
//        )
//
//        jwt.sign(signer)
//        val jwtString = jwt.serialize()
//    }

}
