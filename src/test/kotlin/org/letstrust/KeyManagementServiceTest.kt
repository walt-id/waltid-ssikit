package org.letstrust

import com.google.crypto.tink.subtle.X25519
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Before
import org.junit.Test
import org.letstrust.services.key.FileSystemKeyStore
import org.letstrust.services.key.KeyManagementService
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.KeyAgreement
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class KeyManagementServiceTest {

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun checkRequiredAlgorithms() {
        val kms = KeyManagementService
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
        val kms = KeyManagementService
        val keyId = kms.generateEcKeyPair("secp256k1")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("ECDSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateSecp256k1KeyPairNimbusSunTest() {
        val kms = KeyManagementService

        val keyId = kms.generateSecp256k1KeyPairSun()

        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("EC", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateEd25519KeyPairNimbusSunTest() {
        val kms = KeyManagementService

        val keyId = kms.generateEd25519KeyPairNimbus()
    }

    @Test
    fun generateEd25519KeyPairTest() {
        val kms = KeyManagementService
        val keyId = kms.generateKeyPair("Ed25519")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair?.private?.encoded)
        assertNotNull(keysLoaded?.pair?.public?.encoded)
        val pubKey = keysLoaded?.pair?.public?.encoded
        assertEquals(32, pubKey?.size)
        assertTrue(kms.getMultiBase58PublicKey(keyId).length > 32)
        kms.deleteKeys(keyId)
    }

    @Test
    fun generateRsaKeyPairTest() {
        val kms = KeyManagementService
        val ks = FileSystemKeyStore
        val keyId = kms.generateKeyPair("RSA")
        val keysLoaded = kms.loadKeys(keyId)
        assertEquals(keyId, keysLoaded?.keyId)
        assertNotNull(keysLoaded?.pair)
        assertNotNull(keysLoaded?.pair?.private)
        assertNotNull(keysLoaded?.pair?.public)
        assertEquals("RSA", keysLoaded?.pair?.private?.algorithm)
        kms.deleteKeys(keyId)
    }

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

    // @Test
    //   fun sfd() {
//        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("XDH");
//        val paramSpec: NamedParameterSpec = NamedParameterSpec("X25519");
//        kpg.initialize(paramSpec); // equivalent to kpg.initialize(255)
//        // alternatively: kpg = KeyPairGenerator.getInstance("X25519")
//        val kp1: KeyPair = kpg.generateKeyPair();
//        val kp2: KeyPair = kpg.generateKeyPair();
//
//        val kf: KeyFactory = KeyFactory.getInstance("XDH");
//        val pubSpec: XECPublicKeySpec = XECPublicKeySpec(paramSpec, kp1.);
//        val pubKey = kf.generatePublic(pubSpec);
//
//        val ka = KeyAgreement.getInstance("XDH");
//        ka.init(kp.getPrivate());
//        ka.doPhase(pubKey, true);
//        val secret = ka.generateSecret();
//    }


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


}