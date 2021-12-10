package id.walt.crypto

import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.SunCryptoService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

class SunCryptoTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val sunCryptoService = SunCryptoService()
    val data = "some data".toByteArray()

    @Test
    fun signSecp256k1Test() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        val sig = sunCryptoService.sign(keyId, data)
        val res = sunCryptoService.verify(keyId, sig, data)
        res shouldBe true
    }

    // Ed25519 only supported with Java 15
    @Test
    fun signEd25519Test() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        val sig = sunCryptoService.sign(keyId, data)
        val res = sunCryptoService.verify(keyId, sig, data)
        res shouldBe true
    }

    @Test
    fun signRsaTest() {
        val keyId = sunCryptoService.generateKey(KeyAlgorithm.RSA)
        val sig = sunCryptoService.sign(keyId, data)
        val res = sunCryptoService.verify(keyId, sig, data)
        res shouldBe true
    }
}
