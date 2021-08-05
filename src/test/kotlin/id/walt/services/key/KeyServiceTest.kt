package id.walt.services.key

import com.beust.klaxon.Klaxon
import com.google.crypto.tink.subtle.X25519
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import id.walt.servicematrix.ServiceMatrix
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.newKeyId
import id.walt.model.Jwk
import id.walt.services.crypto.CryptoService
import id.walt.services.keystore.KeyType
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.math.BigInteger
import java.security.*
import java.security.spec.*
import java.util.*
import javax.crypto.KeyAgreement


class KeyServiceTest : AnnotationSpec() {

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
        ServiceMatrix("service-matrix.properties")
    }

    @Test
    fun checkRequiredAlgorithms() {
        var secp256k1 = false
        var p521 = false
        keyService.getSupportedCurveNames().forEach {
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
        secp256k1 shouldBe true
        p521 shouldBe true
    }

    @Test
    fun generateSecp256k1KeyPairTest() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        keyId shouldBe key.keyId
        KeyAlgorithm.ECDSA_Secp256k1 shouldBe key.algorithm
        key.keyPair shouldNotBe null
        key.keyPair?.private shouldNotBe null
        key.keyPair?.public shouldNotBe null
        "ECDSA" shouldBe key.keyPair?.private?.algorithm
        keyService.delete(keyId.id)
    }

    @Test
    fun generateEd25519KeyPairTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        keyId shouldBe key.keyId
        KeyAlgorithm.EdDSA_Ed25519 shouldBe key.algorithm
        key.keyPair shouldNotBe null
        key.keyPair?.private shouldNotBe null
        key.keyPair?.public shouldNotBe null

        keyService.delete(keyId.id)
    }

    @Test
    fun generateEd25519JwkTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)

        val jwk = keyService.toEd25519Jwk(key)
        println(jwk)
        "EdDSA" shouldBe jwk.algorithm.name
        "Ed25519" shouldBe jwk.curve.name

        val jwk2 = keyService.toJwk(key.keyId.id)
        keyId.id shouldBe jwk2.keyID

        keyService.delete(keyId.id)
    }

    @Test
    fun generateSecp256k1JwkTest() {
        // Test generation
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)

        val jwk = keyService.toSecp256Jwk(key)
        println(jwk)
        "ES256K" shouldBe jwk.algorithm.name
        "secp256k1" shouldBe jwk.curve.name

        val jwk2 = keyService.toJwk(key.keyId.id)
        keyId.id shouldBe jwk2.keyID

        keyService.delete(keyId.id)
    }

    @Test
    fun serizalizeEd25519k1JwkTest() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        val jwk = keyService.toEd25519Jwk(key)

        val serializedJwk = Klaxon().parse<Jwk>(jwk.toString())!!
        "EdDSA" shouldBe serializedJwk.alg

        val jwkFromSerialzed = JWK.parse(Klaxon().toJsonString(serializedJwk))

        jwk shouldBe jwkFromSerialzed

        keyService.delete(keyId.id)
    }

    @Test
    fun serizalizeSecp256k1JwkTest() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        val jwk = keyService.toSecp256Jwk(key)

        val serializedJwk = Klaxon().parse<Jwk>(jwk.toString())!!
        "ES256K" shouldBe serializedJwk.alg

        val jwkFromSerialzed = JWK.parse(Klaxon().toJsonString(serializedJwk))

        jwk shouldBe jwkFromSerialzed

        keyService.delete(keyId.id)
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
//        val keyService = KeyManagementService
//        val ks = FileSystemKeyStore
//        val keyId = keyService.generateKeyPair("RSA")
//        val key = keyService.loadKeys(keyId)
//        keyId shouldBe key?.keyId
//        key?.pair shouldNotBe null
//        key?.pair?.private shouldNotBe null
//        key?.pair?.public shouldNotBe null
//        "RSA" shouldBe key?.pair?.private?.algorithm
//        keyService.delete(keyId)
//    }

    @Test
    fun keyAgreementTest() {
        val privKey1 = X25519.generatePrivateKey()
        val pubKey1 = X25519.publicFromPrivate(privKey1)
        val privkey2 = X25519.generatePrivateKey()
        val pubKey2 = X25519.publicFromPrivate(privkey2)

        val sharedSecret1 = String(X25519.computeSharedSecret(privKey1, pubKey2))
        val sharedSecret2 = String(X25519.computeSharedSecret(privkey2, pubKey1))

        sharedSecret1 shouldBe sharedSecret2
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
        keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).let { keyId ->
            keyService.load(keyId.id, KeyType.PRIVATE).keyPair.let { keyPair ->
                val addressFromKeyPair = Keys.toChecksumAddress(Keys.getAddress(ECKeyPair.create(keyPair)))
                val calculatedAddress = keyService.getEthereumAddress(keyId.id)
                addressFromKeyPair shouldBe calculatedAddress
            }
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGetEthereumAddressWithBadKeyAlgorithm() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        keyService.getEthereumAddress(keyId.id)
    }

    @Test
    fun testGetRecoveryId() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val data = "Test data".toByteArray()
        val signature = cryptoService.signEthTransaction(keyId, data)
        val recoveryId = keyService.getRecoveryId(keyId.id, data, signature)
        assert(arrayOf(0, 1, 2, 3).contains(recoveryId))
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadKey() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val badKeyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val data = "Test data".toByteArray()
        val signature = cryptoService.signEthTransaction(keyId, data)
        keyService.getRecoveryId(badKeyId.id, data, signature)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadSignature() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val badSignature = ECDSASignature(
            BigInteger("999"),
            BigInteger("5390839579382847000243128974640652114050572986153482093796582175013638805313")
        )
        keyService.getRecoveryId(keyId.id, "Test data".toByteArray(), badSignature)
    }

    @Test(expected = IllegalStateException::class)
    fun testGetRecoveryIdFailsWithBadData() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val signature = cryptoService.signEthTransaction(keyId, "Test data".toByteArray())
        keyService.getRecoveryId(keyId.id, "Bad data".toByteArray(), signature)
    }

    @Test
    fun testImportEd25519JwkPrivKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"OKP\",\"d\":\"GoVhqvYKbjpzDDRHsBLEIwZTiY39fEpVtXAxKVxKcCg\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"cU4CewjU2Adq8pxjfObrVg9u8svRP2JRC72zZdvFftI\",\"alg\":\"EdDSA\"}"
        keyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK, KeyType.PRIVATE)
        print(jwkExported)
        jwkImport shouldBe jwkExported
    }

    @Test
    fun testImportEd25519JwkPubKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"OKP\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"cU4CewjU2Adq8pxjfObrVg9u8svRP2JRC72zZdvFftI\",\"alg\":\"EdDSA\"}"
        keyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK)
        print(jwkExported)
        jwkImport shouldBe jwkExported
    }

    @Test
    fun testImportSecp256k1JwkPubKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"secp256k1\",\"kid\":\"${kid}\",\"x\":\"ZxPG-mkME3AE19H-_-Z0vQacNTtD_4rChcUJqoiJZ5w\",\"y\":\"EPS4M1CiFoi-psyUNR8otGoNOCm0OvQY_i4fxf4shJY\",\"alg\":\"ES256K\"}"
        keyService.import(jwkImport)
        println(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK)
        print(jwkExported)
        jwkImport shouldBe jwkExported
    }
}
