package org.letstrust

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.junit.Test
import org.letstrust.crypto.CryptoService
import org.letstrust.crypto.JwtSigner
import org.letstrust.crypto.LetsTrustProvider
import org.letstrust.crypto.PrivateKeyHandle
import org.letstrust.services.key.KeyManagementService
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.security.Signature


// SignatureSpi()


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


        jwt.sign(JwtSigner(keyId))
//
//        // Output the JWT
        println(jwt.serialize())
    }

    @Test
    fun testProviderSign() {

        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

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


        ///val signer = JwtSigner(keyId)
        //Security.addProvider(LetsTrustProvider())
        val privateKeyHandle = PrivateKeyHandle(keyId)
        val signer = ECDSASigner(privateKeyHandle, Curve.SECP256K1)
        signer.jcaContext.provider = LetsTrustProvider()
        val sig = jwt.sign(signer)
        println(jwt.serialize())

    }

    @Test
    @Throws(Exception::class)
    fun testProviders() {

        Security.addProvider(LetsTrustProvider());

        val mysig = Signature.getInstance("SHA256withECDSA", "LetsTrust")
        println(mysig)

        val myks = KeyStore.getInstance("PKCS11", "LetsTrust")
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
