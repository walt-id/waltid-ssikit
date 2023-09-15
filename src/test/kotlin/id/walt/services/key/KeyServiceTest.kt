package id.walt.services.key

import com.beust.klaxon.Klaxon
import com.google.crypto.tink.subtle.X25519
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.JWK
import id.walt.crypto.KeyAlgorithm
import id.walt.model.Jwk
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import java.io.File
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
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    @Test
    fun testInMemoryKeyService() {
        InMemoryKeyService.getService().cryptoService::class shouldBe SunCryptoService::class
        InMemoryKeyService.getService().keyStore::class shouldBe InMemoryKeyStoreService::class

        val keyId = InMemoryKeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)
        InMemoryKeyService.getService().listKeys().map { it.keyId } shouldContain keyId
        InMemoryKeyService.getService().delete(keyId.id)
        InMemoryKeyService.getService().listKeys().map { it.keyId } shouldNotContain keyId
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

        val jwk = keyService.toSecp256Jwk(key, Curve.SECP256K1)
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
        val jwk = keyService.toSecp256Jwk(key, Curve.SECP256K1)

        val serializedJwk = Klaxon().parse<Jwk>(jwk.toString())!!
        "ES256K" shouldBe serializedJwk.alg

        val jwkFromSerialzed = JWK.parse(Klaxon().toJsonString(serializedJwk))

        jwk shouldBe jwkFromSerialzed

        keyService.delete(keyId.id)
    }

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
    fun testImportJWKKey() {
        forAll(
//            Ed25519 Private
            row(
                File("src/test/resources/cli/privKeyEd25519Jwk.json").readText().replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PRIVATE
            ),
//            Ed25519 Public
            row(
                File("src/test/resources/cli/pubKeyEd25519Jwk.json").readText().replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PUBLIC
            ),
//            Secp256k1 Private
            row(
                File("src/test/resources/key/privKeySecp256k1Jwk.json").readText().replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PRIVATE
            ),
//            Secp256k1 Public
            row(
                File("src/test/resources/key/pubKeySecp256k1Jwk.json").readText().replace(Regex("(\r\n|\r|\n| )"), ""),
                KeyType.PUBLIC
            ),
//            RSA Private
            row(File("src/test/resources/key/privkey.jwk").readText().replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PRIVATE),
//            RSA Public
            row(File("src/test/resources/key/pubkey.jwk").readText().replace(Regex("(\r\n|\r|\n| )"), ""), KeyType.PUBLIC),
        ) { keyStr, type ->
            val kid = keyService.importKey(keyStr)
            val export = keyService.export(kid.id, KeyFormat.JWK, type)
            print(export)
            export shouldBe keyStr
        }
    }

    @Test
    fun testImportPEMKey() {
        forAll(
//            RSA
            row(File("src/test/resources/key/pem/rsa/rsa.pem").readText(), 0x11),
            row(File("src/test/resources/key/pem/rsa/rsa.public.pem").readText(), 0x10),
            row(File("src/test/resources/key/pem/rsa/rsa.private.pem").readText(), 0x01),
//            Ed25519
            row(File("src/test/resources/key/pem/ed25519/ed25519.pem").readText(), 0x11),
            row(File("src/test/resources/key/pem/ed25519/ed25519.public.pem").readText(), 0x10),
            row(File("src/test/resources/key/pem/ed25519/ed25519.private.pem").readText(), 0x01),
//            Secp256k1
            row(File("src/test/resources/key/pem/ecdsa/secp256k1.pem").readText(), 0x11),
            row(File("src/test/resources/key/pem/ecdsa/secp256k1.public.pem").readText(), 0x10),
            row(File("src/test/resources/key/pem/ecdsa/secp256k1.private.pem").readText(), 0x01),
        ) { keyStr, hasBothKeys ->
            val kid = keyService.importKey(keyStr)
            when (hasBothKeys and 0x11) {
                0x11 -> {
                    keyService.export(kid.id, KeyFormat.PEM, KeyType.PRIVATE).plus(System.lineSeparator())
                        .plus(keyService.export(kid.id, KeyFormat.PEM, KeyType.PUBLIC)) shouldBe keyStr
                }
                0x01 -> {
                    keyService.export(kid.id, KeyFormat.PEM, KeyType.PRIVATE) shouldBe keyStr
                    shouldNotThrow<Exception> { keyService.export(kid.id, KeyFormat.PEM, KeyType.PUBLIC) }
                }
                0x10 -> {
                    keyService.export(kid.id, KeyFormat.PEM, KeyType.PUBLIC) shouldBe keyStr
                }
            }
        }
    }

    @Test
    fun testDeleteKey() {
        val kid = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        keyService.delete(kid.id)
        shouldThrow<Exception> {
            keyService.load(kid.id)
        }
    }

    // https://github.com/AdoptOpenJDK/openjdk-jdk11/blob/master/test/jdk/sun/security/ec/xec/TestXDH.java
    @Test
    fun testKeyAgreementAlgorithms() {
        runKeyAgreementTest("XDH", null)
        runKeyAgreementTest("XDH", 255)
        runKeyAgreementTest("XDH", 448)
        runKeyAgreementTest("XDH", "X25519")
        runKeyAgreementTest("XDH", "X448")
        runKeyAgreementTest("X25519", null)
        runKeyAgreementTest("X448", null)
        runKeyAgreementTest("1.3.101.110", null)
        runKeyAgreementTest("1.3.101.111", null)
        runKeyAgreementTest("OID.1.3.101.110", null)
        runKeyAgreementTest("OID.1.3.101.111", null)
    }

    @Throws(Exception::class)
    fun runKeyAgreementTest(name: String, param: Any?) {
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
