package id.walt.services.keystore

import id.walt.crypto.KeyAlgorithm
import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.crypto.SunCryptoService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe

open class FileSystemKeyStoreServiceTest : AnnotationSpec() {//: KeyStoreServiceTest() {

    private val sunCryptoService = SunCryptoService()
    private val fileSystemKeyStoreService = FileSystemKeyStoreService()
    private val sqlKeyStoreService = SqlKeyStoreService()
    private val keyService = KeyService.getService()

    @BeforeAll
    fun setUp() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
        ServiceRegistry.registerService<KeyStoreService>(FileSystemKeyStoreService())
        sunCryptoService.setKeyStore(fileSystemKeyStoreService)
    }

    @AfterAll
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

        fileSystemKeyStoreService.delete(keyId1.id)
        fileSystemKeyStoreService.delete(keyId2.id)
    }
}
