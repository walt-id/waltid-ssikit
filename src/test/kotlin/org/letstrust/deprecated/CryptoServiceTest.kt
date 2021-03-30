package org.letstrust.deprecated

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.ECDSASigner
import com.nimbusds.jose.crypto.ECDSAVerifier
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import junit.framework.Assert.assertTrue
import org.junit.Test
import org.letstrust.KeyAlgorithm
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.*
import org.letstrust.services.key.KeyManagementService
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.security.Signature
import javax.crypto.Cipher


class CryptoServiceTest {

    @Test
    fun testGenSecp256k1Sun() {
        val keyId = SunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
    }


    @Test
    fun testGenEd25519Tink() {

        val keyId = TinkCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)

    }

    @Test
    fun testJwtSigner() {

        val keyId = KeyManagementService.generateEd25519KeyPairNimbus()

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

        jwt.sign(JwtSigner(keyId))

//        // Output the JWT
        println(jwt.serialize())
    }

    @Test
    fun testProviderSignJwt() {

        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()

        val claimsSet = JWTClaimsSet.Builder()
            .subject("alice")
            .build()

        val jwt = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.ES256K)
                .keyID(keyId)
                .build(),
            claimsSet
        )

        ///val signer = JwtSigner(keyId)
        //Security.addProvider(LetsTrustProvider())
        val privateKeyHandle = PrivateKeyHandle(KeyId(keyId))
        val signer = ECDSASigner(privateKeyHandle, Curve.SECP256K1)
        signer.jcaContext.provider = LetsTrustProvider()
        jwt.sign(signer)

        println(jwt.serialize())

        val jwt2 = SignedJWT.parse(jwt.serialize())

        val pubKey = KeyManagementService.loadKeys(keyId)!!.toEcKey().toECPublicKey()
        val verifier = ECDSAVerifier(pubKey)
        assertTrue(jwt2.verify(verifier))


        println(jwt2.jwtClaimsSet.toJSONObject())

    }

    @Test
    @Throws(Exception::class)
    fun testPrintProviders() {

        LetsTrustServices

        Security.addProvider(LetsTrustProvider());

        val mysig = Signature.getInstance("SHA256withECDSA", "LetsTrust")
        println(mysig)

        val myks = KeyStore.getInstance("PKCS11", "LetsTrust")
        println(myks)

        val mycipher = Cipher.getInstance("AES/GCM/NoPadding", "LetsTrust")
        println(mycipher)

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
