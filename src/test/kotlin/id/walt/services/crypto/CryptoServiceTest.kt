package id.walt.services.crypto

import id.walt.crypto.*
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec


class CryptoServiceTest : AnnotationSpec() {

    val sunCryptoService = SunCryptoService()
    val tinkCryptoService = TinkCryptoService()

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    @Test
    fun testGenSecp256k1Sun() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        keyId.id shouldNotBe null
    }

    @Test
    fun testGenEd255191Sun() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.RSA)
        keyId.id shouldNotBe null
    }

    @Test
    fun testHexEncoding() {
        val inString = "my bytes"
        val inBytes = inString.toByteArray()
        val hex = inBytes.toHexString()
        val outBytes = hex.fromHexString()
        outBytes shouldBe inBytes
        String(outBytes) shouldBe inString
    }

    @Test
    fun testGenEd25519Tink() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        keyId.id shouldNotBe null
        File("data/key/$keyId.tink").delete()
    }

    // @Test TODO: not supported yet https://github.com/google/tink/issues/146
    fun testGenSecp256k1Tink() {
        val keyId = tinkCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        keyId.id shouldNotBe null
    }

    @Test
    fun testAddWaltIdProviders() {

        WaltIdServices

        Security.addProvider(WaltIdProvider())

        println("SupportedCurves")
        println(
            "SupportedCurves: -> SunEC: " + Security.getProvider("SunEC")
                .getProperty("AlgorithmParameters.EC SupportedCurves")
        )


        var waltIdProviderFound = false

        val mysig = Signature.getInstance("SHA256withECDSA", "Walt")
        println(mysig)

        val myks = KeyStore.getInstance("PKCS11", "Walt")
        println(myks)

//        val mycipher = Cipher.getInstance("AES/GCM/NoPadding", "Walt")
//        println(mycipher)

        val providers: Array<Provider> = Security.getProviders()

        for (p in providers) {
            val info: String = p.info
            println("$p - $info")

            if (p.toString().contains("Walt", true)) {
                waltIdProviderFound = true
            }

            p.services.forEach { s ->
                if (s.toString().contains("Walt", true)) {
                    println("\t -> $s")
                }
            }
        }
        waltIdProviderFound shouldBe true
    }

    @Test
    fun testSignVerifySun() {
        val data = "Sign Me!".toByteArray()

        val keyId = sunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)

        for (i in 1..10) {
            val signature = sunCryptoService.sign(keyId, data)
            val verify = sunCryptoService.verify(keyId, signature, data)
            verify shouldBe true
        }
    }

    @Test
    fun testEncWithAes() {
        val data = "Encrypt Me!"
        val encKey = KeyGenerator.getInstance("AES").generateKey()
        val encCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        encCipher.init(Cipher.ENCRYPT_MODE, encKey)
        val enc = encCipher.doFinal(data.toByteArray())

        val encodedKey = encBase64(encKey.encoded)
//        val ap = encCipher.parameters // IV
        val decKey = SecretKeySpec(decBase64(encodedKey), "AES")
        val decCipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        decCipher.init(Cipher.DECRYPT_MODE, decKey)
        val decData = String(decCipher.doFinal(enc))

        data shouldBe decData
    }

}
