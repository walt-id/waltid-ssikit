package id.walt.services.keystore

import id.walt.crypto.KeyAlgorithm
import id.walt.services.key.KeyService
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.*

class SqlKeyStoreServiceTest : KeyStoreServiceTest() {

    private val sqlKeyStoreService = SqlKeyStoreService()
    private val keyService = KeyService.getService()

    @Test
    fun addAliasSqlApiTest() {
        val keyId = keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        sqlKeyStoreService.store(keyService.load(keyId.id))
        val alias = UUID.randomUUID().toString()
        sqlKeyStoreService.addAlias(keyId, alias)
        val k1 = sqlKeyStoreService.getKeyId(alias)
        k1 shouldNotBe null
        keyId.id shouldBe k1
        val k2 = sqlKeyStoreService.load(keyId.id)
        k2 shouldNotBe null
        k1 shouldBe k2.keyId.id
    }


    // TODO refactore following test
//    @Test
//    fun saveLoadByteKeysSqlApiTest() {
//        val priv = BytePrivateKey("priv".toByteArray(), "alg")
//        val pub = BytePublicKey("pub".toByteArray(), "alg")
//        val keys = Keys(UUID.randomUUID().toString(), KeyPair(pub, priv), "dummy")
//        SqlKeyStoreService.saveKeyPair(keys)
//        val keysLoaded = SqlKeyStoreService.load(keys.keyId)
//        keysLoaded shouldNotBe null
//        keys.keyId shouldBe keysLoaded.keyId
//        "priv" shouldBe String(keysLoaded.pair.private.encoded)
//        "pub" shouldBe String(keysLoaded.pair.public.encoded)
//        "alg" shouldBe keysLoaded.algorithm
//        "byte" shouldBe keysLoaded.pair.private.format
//    }


}
