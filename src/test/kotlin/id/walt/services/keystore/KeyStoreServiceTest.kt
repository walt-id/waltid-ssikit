package id.walt.services.keystore

import id.walt.servicematrix.ServiceMatrix
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import id.walt.crypto.KeyAlgorithm
import id.walt.services.key.KeyService
import java.security.Security
import java.util.*

open class KeyStoreServiceTest : AnnotationSpec() {

    private val keyService = KeyService.getService()

    init {
        Security.addProvider(BouncyCastleProvider())
        ServiceMatrix("service-matrix.properties")
    }

    /* ServiceLoader is deprecated, no longer used, and will be removed in a future version
    @Test
    fun serviceLoaderTest() {
        val loader = ServiceLoader.load(KeyStoreService::class.java)
        val ksServiceLoader = loader.iterator().next()
        println(ksServiceLoader)

        val ksKClass = Class.forName("id.walt.services.keystore.CustomKeyStoreService").kotlin.createInstance()
        println(ksKClass)

        val ksObject = FileSystemKeyStoreService
        println(ksObject)
    }*/

    @Test
    open fun addAliasTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val testAlias = UUID.randomUUID().toString()
        keyService.addAlias(keyId, testAlias)
        val k1 = keyService.load(testAlias, KeyType.PRIVATE)
        k1 shouldNotBe null
        val k2 = keyService.load(keyId.id, KeyType.PRIVATE)
        k2 shouldNotBe null
        k2.getPublicKey().encoded.contentToString() shouldBe k1.getPublicKey().encoded.contentToString()
    }

    @Test
    open fun saveLoadEd25519KeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        48 shouldBe key.keyPair!!.private.encoded.size
    }

    @Test
    open fun saveLoadSecp256k1KeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        88 shouldBe key.keyPair!!.public.encoded.size
        144 shouldBe key.keyPair!!.private.encoded.size
    }

    @Test
    fun listKeysTest() {
        var keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        println("keys: " + keyService.listKeys().joinToString { it.toString() })
    }

    @Test
    open fun deleteKeysTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        val key = keyService.load(keyId.id, KeyType.PRIVATE)
        keyService.delete(key.keyId.id)

        shouldThrow<Exception> {
            keyService.load(keyId.id, KeyType.PRIVATE)
        }
    }
}
