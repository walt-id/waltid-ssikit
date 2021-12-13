package id.walt.services.key

import com.beust.klaxon.Klaxon
import com.google.crypto.tink.subtle.X25519
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.newKeyId
import id.walt.model.Jwk
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
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
    fun testImportExportEd25519JwkPrivKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"OKP\",\"d\":\"NzNkDxp2OPyplpxvxSmKtHCul2tQ_7QNuameOTKd6uY\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"4t6ROMKS2g9hwguVM-u9LzR06spoS__YyaOOvrtSFiI\",\"alg\":\"EdDSA\"}"
        keyService.importKey(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK, KeyType.PRIVATE)
        jwkImport shouldBe jwkExported
    }

    @Test
    fun testImportExportEd25519JwkPubKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"OKP\",\"use\":\"sig\",\"crv\":\"Ed25519\",\"kid\":\"${kid}\",\"x\":\"cU4CewjU2Adq8pxjfObrVg9u8svRP2JRC72zZdvFftI\",\"alg\":\"EdDSA\"}"
        keyService.importKey(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK)
        jwkImport shouldBe jwkExported
    }

    @Test
    fun testImportExportSecp256k1JwkPubKey() {
        val kid = newKeyId()
        val jwkImport =
            "{\"kty\":\"EC\",\"use\":\"sig\",\"crv\":\"secp256k1\",\"kid\":\"${kid}\",\"x\":\"ZxPG-mkME3AE19H-_-Z0vQacNTtD_4rChcUJqoiJZ5w\",\"y\":\"EPS4M1CiFoi-psyUNR8otGoNOCm0OvQY_i4fxf4shJY\",\"alg\":\"ES256K\"}"
        keyService.importKey(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK)
        jwkImport shouldBe jwkExported
    }

    @Test
    fun testImportExportRsaJwkPrivKey() {
        val kid = newKeyId()
        val jwkImport = "{\"alg\":\"RSA\",\"p\":\"5_LP2MRAr_9M6PDONj3uU4qJfHZKVqTVyhadZKKxUg5a8DZgIac_Ies_XoDEs7UbxfFXQuWSSC0gwxOOkm6HkzoxxA7IxB_wn0vhnaf4iUiNGXCqCm-DlcP8eL9Lh6Y42AayNIPwTsaHBhZxCLx3l59EzNKweRRsEGZoS1dgQJE\",\"kty\":\"RSA\",\"q\":\"5vf-g7dpwpfHWT8Lo2mJKFL-5F0BAD4L8YnTV06I-D2Uylfxsqs6W4Sqq5qNzmnrs0mELdAqf37Xa4XP0mSVaI-gQ9knTf4uDkqADJD4u3Q5k_b2KtsmmVkyObw_c3KauGyTaT6Li_u1jmy4-98814-BlPTZgroSRe9dLaBmAVc\",\"d\":\"y-rPPW4ODc_kRfO45wOixXRIJg9xdWpy6NpFcA-t0lMUnxUpC_TdCuc2UxLXkppt1uEA_qh6uXVTDB_-fS-OblEek0MrlSG7ocG3K91GVjvAk5dNYy5m0KPa_lp95aD26gee3zGx8ppNHO0TAzt9HcRyXWfeGv21ogDF7cVcsI0PcJVJAdpSwnhzpjkoxTAwTr1rb_VLj_E_LyN68bggTD0cDKXQ6Bx3Mbg-5hthp0OhoBOS05t-QSH5cNX2mycVaP8Pje16nvEiw1ltCl2ugEj4UYFUybD75ZUHtCw47ee0hTJoVdbrKDNzgLqapyseZse0rCaXkb1ovc5puEfL4Q\",\"e\":\"AQAB\",\"kid\":\"d9f43e84-efc9-49eb-8715-63ae731008a3\",\"qi\":\"xPy0udiVAzNc-cgo3nd-iyzblvucps_g9TJfajFpCPYqBzde_doC31I-nXdIol9RxCdKBSkKlozV-Vsts2UAO4DtSbmP3cxtH-pSp9yIqUjOen97w_4gCuBcHtE4KKbuOgMCzozmVvn6sCjhkjh7oTCwNpjLMJLJPd1iqeJBsUg\",\"dp\":\"57xzJmolCvGyIIT-Mbk8VGD0LdJtQRWctzRS-gmFyaqn9pkNAHJ9I-FKRZu3aqhGYERYX3DH7q6PrfbrGaeuckzRDcWLlk8m87A3cHEyYc6HkmQ6rwRs3gOaSfUtfBB5eHNwNgGf5MR6gH2JXyYVAfpRHaZeRApAUT5Pcv6QHtE\",\"dq\":\"a6_cvtTZPp09mOLILlyaUm6_4QFr4g0LzIYSP3aibftoUB9I1aD4CIuGd5QL4d2Iw4LXWfTgm6ksDznId7Pl5WZxtrCcnsSP_KHHqhQ9pEjAP7i5danQCVeJD1oxy0X31VzqLfu3XIDzWYBfjy-6Ulbad_ThJQ5UTr8XlppedOk\",\"n\":\"0UTYnYwfTKiEshEUnJkn4QRU8iPodt2khsshXaTTVLxTJjTDW1R6vuM59ok-okGj7N2mHYzRwmLpsHiwRoufKDvMJRh146XOon6K5FiBSW2GbekEm6IXjzCOVX7hbROLbDuzhnE37TlmR2-XEb-hLNHQ9AbknKjVS0oFPt2egghPc3EWy9hjzRv84QwMFcg7s6QBxsUgQKcVAJuen5g9A2N1DVCe-FH8Q-61RZpueR403Dl1yBpDL7-jNB5SCUwzSgpxkD1L8_SNrAuZfT_sPLASF-c8UKQOnRmVoPSy6KDW_LGxb3fxJ-ZeIYdMUa3bud6Uj6igAzhNlbwja7yCRw\"}"
        keyService.importKey(jwkImport)
        println(jwkImport)
        val jwkExported = keyService.export(kid.id, KeyFormat.JWK)
        print(jwkExported)
        jwkImport shouldBe jwkExported
    }

    fun testImportExportRsaPemPrivKey() {
        val key = "-----BEGIN RSA PRIVATE KEY-----\n" +
                "MIIEowIBAAKCAQEAtzmiw+nf5UO+ogR2GT9JboMQz5tie3TXtquygsqdkk29UaLv\n" +
                "51Im7w+Audj2TOoXp2ukA752BZo4wSLapGNZQxB32frfSqgZUxIpXWdMFzL9JwgR\n" +
                "JMS8e/nuoZDL0Jy54TAO7IJmRHVM4i7JZmCH61WWLHwFCmzL+U2BdJig1gNPhRpN\n" +
                "aXXiwAgH59WyviqF9rX80e3aMcaH+smHb8GUHscec5ctva88kH/VRdxjlhZqygIT\n" +
                "dFZagjhSJ+djchD0gPYqRuuqHPWN0QgdoEf8nhi5DexcyJ1V6RgAv5lkzVICPIen\n" +
                "OCu1q6J24KU35gSNqID3pF/Kr0DilDkKBcLu1QIDAQABAoIBAGkR5iLO5RQGCzXB\n" +
                "tS+5ORTkmClVg94kHOemAlI6eq3BYsWD2Gsgky8YBsuMfYGR5Eqf0YhMGkYQMGeg\n" +
                "4xzN1Aw/T1tzH8UiLJOUoJ/tcpcDKGTPnXUmVKgLpSqFbDuPBJD8DDLYfGjZk2NJ\n" +
                "TTkmNgtgIyQTYpid10J5jbkdJW5Tst193y3B4mBEofiONOGLwMBw5n4/NSLaRcS9\n" +
                "nJs74uIji84NA1geY5vCG475PZz6hBX7XCs8R3b8AVajYXekcpWr9OJjUAPppyGf\n" +
                "/OfsaOKwPiopeOAedtETkLShB/bm4L/br6ti70itn/HEi9HNM6rcnOHwJVvGQAyH\n" +
                "yqb+yUECgYEA5aygwGS+6EoUaJwXfQkGcAyMlWm/TodjC5h7NaByUhjbPTJn4c1G\n" +
                "Hlvns5XZKKdlC9PHfHisUf9WS7JLbWEJusxcObnwl8ueqAAEaXDw7vZrMsEOsrl9\n" +
                "b05YlrGpGXaOLOy7OQ83C+cFRM2SA4qrpr+2fGQQWMRmjG3HSRWBPckCgYEAzDoK\n" +
                "ppCphHPcQZlCf4yC4E4C1E5Fmc5wxzmP6GKTpbTYQyt2l78U4q7lDgV7NXDL++3M\n" +
                "twIJdh4gdbD2TFZi4hB9ClL4aYb9wHO2e2OTzw+aqe/K1FlukgTfXOuHAewPROkn\n" +
                "42yUMLY8KhvfWyr9iFIV3LzPEzv71oh6/vmJvq0CgYB0QwGQwq7c+XsBRVqigaoP\n" +
                "mFql28TqpKAfo41jJRgZtNluThDF/dprzcwpXUZzTOFarlbCDHf2fhGZ+eQytzds\n" +
                "prxcwGIpBPsIQhH5qiFcZcL4C0A8eqcja/5uMfrOl/P6i89uX+RWkxhYrtMmFdE0\n" +
                "dMGUkDayKKFcnsmNlmQ4+QKBgAnCjFfBehh2YQRRirgFwwttLv1ucC0VjJY4zgPR\n" +
                "EjVNGzi6jwRZgWoD6bZt1KGNLnJvvuTQGBuo/Owi9OJZDoi3OQKRTIXeian03bev\n" +
                "3pR6rm2IpCzZyUr5KKOMLfuNiH1Glz1rJvnc+6sXgekdeNhW8+yEqXDF4Rczlo0w\n" +
                "58BRAoGBALyv4HL7fomfp/yo8OLl1PK3mswgOk/0cs/oJUN0aZ4foVAvYYLM6Wtk\n" +
                "fFaphkjGh0ynGBLiJYXR1MMFtvUe50nE3gapck/GBC5g3q27o6cuGBinTl5kPH9j\n" +
                "M9Gpr6eOFmn6wxxOs8raYkqTSSEGBIiJ2FCHYAwRgevhBsdmDfDt\n" +
                "-----END RSA PRIVATE KEY-----"


    }
}
