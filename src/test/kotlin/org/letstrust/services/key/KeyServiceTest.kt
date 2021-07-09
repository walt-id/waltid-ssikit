package org.letstrust.services.key

import com.google.crypto.tink.subtle.X25519
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.CryptoService
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.newKeyId
import org.letstrust.model.Jwk
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigInteger
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.KeyAgreement
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class KeyServiceTest {

    private val cs = LetsTrustServices.load<CryptoService>()

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun checkRequiredAlgorithms() {
        val kms = KeyService
        var secp256k1 = false
        var p521 = false
        kms.getSupportedCurveNames().forEach {
            // println(it)
            when (it) {
                "secp256k1" -> {
                    secp256k1 = true
                }
                "P-521" -> {
                    p521 = true
                }
            }
        }
        assertTrue(secp256k1)
        assertTrue(p521)
    }

    @Test
    fun generateSecp256k1KeyPairTest() {
        val kms = KeyService
        val keyId = kms.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = kms.load(keyId.id, true)
        assertEquals(keyId, key?.keyId)
        assertEquals(KeyAlgorithm.ECDSA_Secp256k1, key?.algorithm)
        assertNotNull(key?.keyPair)
        assertNotNull(key?.keyPair?.private)
        assertNotNull(key?.keyPair?.public)
        assertEquals("ECDSA", key?.keyPair?.private?.algorithm)
        kms.delete(keyId.id)
    }

    @Test
    fun generateEd25519KeyPairTest() {
        val kms = KeyService
        val keyId = kms.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = kms.load(keyId.id, true)
        assertEquals(keyId, key?.keyId)
        assertEquals(KeyAlgorithm.EdDSA_Ed25519, key?.algorithm)
        assertNotNull(key?.keyPair)
        assertNotNull(key?.keyPair?.private)
        assertNotNull(key?.keyPair?.public)

        kms.delete(keyId.id)
    }

    @Test
    fun generateEd25519JwkTest() {
        val keyId = KeyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = KeyService.load(keyId.id, true)

        val jwk = KeyService.toEd25519Jwk(key)
        println(jwk)
        assertEquals("EdDSA", jwk.algorithm.name)
        assertEquals("Ed25519", jwk.curve.name)

        val jwk2 = KeyService.toJwk(key.keyId.id)
        assertEquals(keyId.id, jwk2.keyID)

        KeyService.delete(keyId.id)
    }

    @Test
    fun generateSecp256k1JwkTest() {
        // Test generation
        val kms = KeyService
        val keyId = kms.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = kms.load(keyId.id, true)

        val jwk = KeyService.toSecp256Jwk(key)
        println(jwk)
        assertEquals("ES256K", jwk.algorithm.name)
        assertEquals("secp256k1", jwk.curve.name)

        val jwk2 = KeyService.toJwk(key.keyId.id)
        assertEquals(keyId.id, jwk2.keyID)

        kms.delete(keyId.id)
    }

    @Test
    fun serizalizeEd25519k1JwkTest() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = KeyService.load(keyId.id, true)
        val jwk = KeyService.toEd25519Jwk(key)

        val serializedJwk = Json.decodeFromString<Jwk>(jwk.toString())
        assertEquals("EdDSA", serializedJwk.alg)

        val jwkFromSerialzed = JWK.parse(Json.encodeToString(serializedJwk))

        assertEquals(jwk, jwkFromSerialzed)

        KeyService.delete(keyId.id)
    }

    @Test
    fun serizalizeSecp256k1JwkTest() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = KeyService.load(keyId.id, true)
        val jwk = KeyService.toSecp256Jwk(key)

        val serializedJwk = Json.decodeFromString<Jwk>(jwk.toString())
        assertEquals("ES256K", serializedJwk.alg)

        val jwkFromSerialzed = JWK.parse(Json.encodeToString(serializedJwk))

        assertEquals(jwk, jwkFromSerialzed)

        KeyService.delete(keyId.id)
    }

    // TODO complete following two tests
    @Test
    fun generateJwkNimbus() {
        // Generate EC key pair in JWK format
        val jwk: ECKey = ECKeyGenerator(Curve.P_256)
            .keyUse(KeyUse.SIGNATURE) // indicate the intended use of the key
            .keyIDFromThumbprint(true)
            .generate()

        // Output the private and public EC JWK parameters
        System.out.println(jwk)

        // Output the public EC JWK parameters only
        System.out.println(jwk.toPublicJWK())
    }

    @Test
    fun generateJwkJava() {
        // Generate EC key pair in JWK format
        // val kp = keyPairGeneratorSecp256k1().generateKeyPair()

        // val jwk = KeyUtil.make(kp)

        // Output the private and public EC JWK parameters
        // System.out.println(jwk)

        // Output the public EC JWK parameters only
        // System.out.println(jwk.toPublicJWK())
    }

//
//    @Test
//    fun generateRsaKeyPairTest() {
//        val kms = KeyManagementService
//        val ks = FileSystemKeyStore
//        val keyId = kms.generateKeyPair("RSA")
//        val key = kms.loadKeys(keyId)
//        assertEquals(keyId, key?.keyId)
//        assertNotNull(key?.pair)
//        assertNotNull(key?.pair?.private)
//        assertNotNull(key?.pair?.public)
//        assertEquals("RSA", key?.pair?.private?.algorithm)
//        kms.delete(keyId)
//    }

    @Test
    fun keyAgreementTest() {
        val privKey1 = X25519.generatePrivateKey()
        val pubKey1 = X25519.publicFromPrivate(privKey1)
        val privkey2 = X25519.generatePrivateKey()
        val pubKey2 = X25519.publicFromPrivate(privkey2)

        val sharedSecret1 = String(X25519.computeSharedSecret(privKey1, pubKey2))
        val sharedSecret2 = String(X25519.computeSharedSecret(privkey2, pubKey1))

        assertEquals(sharedSecret1, sharedSecret2)
    }

    // https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/test/jdk/sun/security/ec/xec/TestXDH.java
    @Test
    fun runBasicTests() {
        runBasicTest("XDH", null)
        runBasicTest("XDH", 255)
        runBasicTest("XDH", 448)
        runBasicTest("XDH", "X25519")
        runBasicTest("XDH", "X448")
        runBasicTest("X25519", null)
        runBasicTest("X448", null)
        runBasicTest("1.3.101.110", null)
        runBasicTest("1.3.101.111", null)
        runBasicTest("OID.1.3.101.110", null)
        runBasicTest("OID.1.3.101.111", null)
    }

    @Throws(Exception::class)
    fun runBasicTest(name: String, param: Any?) {
        val kpg = KeyPairGenerator.getInstance(name)
        var paramSpec: AlgorithmParameterSpec? = null
        if (param is Int) {
            kpg.initialize(param)
        } else if (param is String) {
            paramSpec = NamedParameterSpec(param)
            kpg.initialize(paramSpec)
        }
        val kp = kpg.generateKeyPair()
        val ka = KeyAgreement.getInstance(name)
        ka.init(kp.private, paramSpec)
        ka.doPhase(kp.public, true)
        val secret = ka.generateSecret()
        val kf = KeyFactory.getInstance(name)
        // Test with X509 and PKCS8 key specs
        val pubSpec = kf.getKeySpec(kp.public, X509EncodedKeySpec::class.java)
        val priSpec = kf.getKeySpec(kp.private, PKCS8EncodedKeySpec::class.java)
        val pubKey: PublicKey = kf.generatePublic(pubSpec)
        val priKey: PrivateKey = kf.generatePrivate(priSpec)
        ka.init(priKey)
        ka.doPhase(pubKey, true)
        val secret2 = ka.generateSecret()
        if (!Arrays.equals(secret, secret2)) {
            throw RuntimeException("Arrays not equal")
        }

        // make sure generateSecret() resets the state to after init()
        try {
            ka.generateSecret()
            throw RuntimeException("generateSecret does not reset state")
        } catch (ex: IllegalStateException) {
            // do nothing---this is expected
        }
        ka.doPhase(pubKey, true)
        ka.generateSecret()

        // test with XDH key specs
        val xdhPublic = kf.getKeySpec(kp.public, XECPublicKeySpec::class.java)
        val xdhPrivate = kf.getKeySpec(kp.private, XECPrivateKeySpec::class.java)
        val pubKey2: PublicKey = kf.generatePublic(xdhPublic)
        val priKey2: PrivateKey = kf.generatePrivate(xdhPrivate)
        ka.init(priKey2)
        ka.doPhase(pubKey2, true)
        val secret3 = ka.generateSecret()
        if (!Arrays.equals(secret, secret3)) {
            throw RuntimeException("Arrays not equal")
        }
    }

    @Test
    fun testGetEthereumAddress() {
        KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1).let { keyId ->
            KeyService.load(keyId.id, true).keyPair.let { keyPair ->
                val addressFromKeyPair = Keys.toChecksumAddress(Keys.getAddress(ECKeyPair.create(keyPair)))
                val calculatedAddress = KeyService.getEthereumAddress(keyId.id)
                assertEquals(addressFromKeyPair, calculatedAddress)
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetEthereumAddressWithBadKeyAlgorithm() {
        val keyId = KeyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        KeyService.getEthereumAddress(keyId.id)
    }

    @Test
    fun testGetRecoveryId() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val data = "Test data".toByteArray()
        val signature = cs.signEthTransaction(keyId, data)!!
        val recoveryId = KeyService.getRecoveryId(keyId.id, data, signature)
        assert(arrayOf(0, 1, 2, 3).contains(recoveryId))
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadKey() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val badKeyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val data = "Test data".toByteArray()
        val signature = cs.signEthTransaction(keyId, data)!!
        KeyService.getRecoveryId(badKeyId.id, data, signature)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadSignature() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val badSignature = ECDSASignature(
            BigInteger("999"),
            BigInteger("5390839579382847000243128974640652114050572986153482093796582175013638805313")
        )
        KeyService.getRecoveryId(keyId.id, "Test data".toByteArray(), badSignature)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadData() {
        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val signature = cs.signEthTransaction(keyId, "Test data".toByteArray())!!
        KeyService.getRecoveryId(keyId.id, "Bad data".toByteArray(), signature)
    }

    @Test
    fun testImportEd25519JwkPrivKey() {
        val kid = newKeyId()
        val jwkImport = "{\"kty\":\"OKP\",\"d\":\"GoVhqvYKbjpzDDRHsBLEIwZTiY39fEpVtXAxKVxKcCg\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"cU4CewjU2Adq8pxjfObrVg9u8svRP2JRC72zZdvFftI\",\"alg\":\"EdDSA\"}"
        KeyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = KeyService.export(kid.id, KeyFormat.JWK, true)
        print(jwkExported)
        assertEquals(jwkImport, jwkExported)
    }

    @Test
    fun testImportEd25519JwkPubKey() {
        val kid = newKeyId()
        val jwkImport = "{\"kty\":\"OKP\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"cU4CewjU2Adq8pxjfObrVg9u8svRP2JRC72zZdvFftI\",\"alg\":\"EdDSA\"}"
        KeyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = KeyService.export(kid.id, KeyFormat.JWK)
        print(jwkExported)
        assertEquals(jwkImport, jwkExported)
    }

    @Test
    fun testImportSecp256k1JwkPubKey() {
        val kid = newKeyId()
        val jwkImport = "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"secp256k1\",\"kid\":\"${kid}\",\"x\":\"ZxPG-mkME3AE19H-_-Z0vQacNTtD_4rChcUJqoiJZ5w\",\"y\":\"EPS4M1CiFoi-psyUNR8otGoNOCm0OvQY_i4fxf4shJY\",\"alg\":\"ES256K\"}"
        KeyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = KeyService.export(kid.id, KeyFormat.JWK)
        print(jwkExported)
        assertEquals(jwkImport, jwkExported)
    }
}
