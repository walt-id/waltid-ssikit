package id.walt.crypto.keystore

import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import id.walt.crypto.KeyAlgorithm
import id.walt.services.crypto.SunCryptoService
import id.walt.services.key.KeyService
import id.walt.services.keystore.FileSystemKeyStoreService
import id.walt.services.keystore.KeyStoreService
import id.walt.services.keystore.SqlKeyStoreService

open class FileSystemKeyStoreTest : AnnotationSpec() {//: KeyStoreTest() {

    val sunCryptoService = SunCryptoService()
    val keyService = KeyService.getService()
    val fileSystemKeyStoreService = FileSystemKeyStoreService()
    val sqlKeyStoreService = SqlKeyStoreService()

    @Before
    fun setUp() {
        ServiceMatrix("service-matrix.properties")
        ServiceRegistry.registerService<KeyStoreService>(FileSystemKeyStoreService())
        sunCryptoService.setKeyStore(fileSystemKeyStoreService)
    }

    @After
    fun tearDown() {
        sunCryptoService.setKeyStore(sqlKeyStoreService)
    }

    @Test
    fun listKeysTest() {
        println("Generating keys...")
        val keyId1 = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        println("Generated EdDSA_Ed25519 key: $keyId1")
        val keyId2 = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        println("Generated ECDSA_Secp256k1 key: $keyId2")

        println("Loading keys...")

        println("Loading EdDSA_Ed25519 key: $keyId1")
        val key1 = fileSystemKeyStoreService.load(keyId1.id)

        println("Generated ECDSA_Secp256k1 key: $keyId2")
        val key2 = fileSystemKeyStoreService.load(keyId2.id)

        keyId1 shouldBe key1.keyId
        keyId2 shouldBe key2.keyId
    }
}
