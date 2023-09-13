package id.walt.services.keystore

import id.walt.services.key.KeyService
import id.walt.crypto.*
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.context.ContextManager
import id.walt.services.did.DidKeyCreateOptions
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.hkvstore.HKVKey
import mu.KotlinLogging
import java.security.PrivateKey
import java.security.PublicKey

open class HKVKeyStoreService : KeyStoreService() {

    private val log = KotlinLogging.logger {}
    open val hkvStore
        get() = ContextManager.hkvStore // lazy load!

    //TODO: get key format from config
    private val KEY_FORMAT = KeyFormat.PEM
    private val KEYS_ROOT = HKVKey("keystore", "keys")
    private val ALIAS_ROOT = HKVKey("keystore", "alias")

    override fun listKeys(): List<Key> = hkvStore.listChildKeys(KEYS_ROOT, recursive = true)
        .filter { k -> k.name == "meta" }
        .map {
            load(it.parent!!.name)
        }

    override fun load(alias: String, keyType: KeyType): Key {
        log.debug { "Loading key \"${alias}\"." }

        val keyId = getKeyId(alias) ?: alias

        val metaData = loadKey(keyId, "meta").decodeToString()
        val algorithm = metaData.substringBefore(delimiter = ";")
        val provider = metaData.substringAfter(delimiter = ";")

        val publicPart = loadKey(keyId, "enc-pubkey").decodeToString()
        val privatePart = if (keyType == KeyType.PRIVATE) loadKey(keyId, "enc-privkey").decodeToString() else null


        return buildKey(keyId, algorithm, provider, publicPart, privatePart, KEY_FORMAT)
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        hkvStore.put(HKVKey.combine(ALIAS_ROOT, alias), keyId.id)
        val aliases =
            hkvStore.getAsString(HKVKey.combine(KEYS_ROOT, keyId.id, "aliases"))?.split("\n")?.plus(alias) ?: listOf(
                alias
            )
        hkvStore.put(HKVKey.combine(KEYS_ROOT, keyId.id, "aliases"), aliases.joinToString("\n"))
    }

    override fun store(key: Key) {
        log.debug { "Storing key \"${key.keyId}\"." }
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storeAvailableKeys(key)
    }

    override fun getKeyId(alias: String) =
        runCatching { hkvStore.getAsString(HKVKey.combine(ALIAS_ROOT, alias)) }.getOrNull()

    override fun delete(alias: String) {
        val keyId = getKeyId(alias)
        if (keyId.isNullOrEmpty())
            return
        val aliases = hkvStore.getAsString(HKVKey.combine(KEYS_ROOT, keyId, "aliases")) ?: ""
        aliases.split("\n").forEach { a -> hkvStore.delete(HKVKey.combine(ALIAS_ROOT, a), recursive = false) }
        hkvStore.delete(HKVKey.combine(KEYS_ROOT, keyId), recursive = true)
    }

    private fun storeAvailableKeys(key: Key) = run {
        key.keyPair?.run {
            this.private?.run { saveKey(key.keyId, this) }
//            this.public?.run { saveKey(key.keyId, this) }
        }
        runCatching { key.getPublicKey() }.onSuccess { saveKey(key.keyId, it) }
    }

    private fun saveKey(keyId: KeyId, key: java.security.Key) = when (key) {
        is PrivateKey -> "enc-privkey"
        is PublicKey -> "enc-pubkey"
        else -> throw IllegalArgumentException()
    }.run {
        saveKeyData(
            keyId, this, when (KEY_FORMAT) {
                KeyFormat.PEM -> key.toPEM()
                else -> key.toBase64()
            }.toByteArray()
        )
    }

    private fun storeKeyMetaData(key: Key) {
        saveKeyData(key.keyId, "meta", (key.algorithm.name + ";" + key.cryptoProvider.name).toByteArray())
    }

    private fun saveKeyData(keyId: KeyId, suffix: String, data: ByteArray): Unit =
        hkvStore.put(HKVKey.combine(KEYS_ROOT, keyId.id, suffix), data)

    private fun loadKey(keyId: String, suffix: String): ByteArray =
        HKVKey.combine(KEYS_ROOT, keyId, suffix)
            .let { hkvStore.getAsByteArray(it) ?: throw NoSuchElementException("Could not load key '$it' from HKV store") }
}

fun main(){
    ServiceMatrix("service-matrix.properties")
//    val keyStr = """
//        -----BEGIN PRIVATE KEY-----
//        MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCXHK07oaqx8fnY
//        r3UbfUS6HRXQFRvQ0J8qqzgq+UH4ZqtgxV44ciSOwzL65E2aZrixXxB+s7Kbbw1q
//        R0oUNvay8QhMlmwUZwXjCZbeNbQI8LXoXSU1l9xx2GZ7BS3/huFGHSyGzjrSYJdJ
//        cZKOYij26aCPIx7VYEeIUmPAbkCA1VVUhaOic81aQAdhqrKjqpBcYTwYW4YF2zcy
//        Dx8YLrRLJbjFzM94eg9oErqIsptyZ83daoNytVTbijzDoXAmkHrx58NfZnuJ0JfH
//        UKGZiMlt6fBAqDR3+Dls6hemJA+VxCO2dKBDp2vSGfIDc1mr1kQozFK3JqFINcWI
//        537vnPWVAgMBAAECggEAA/VAagMFx3k/p/05MMdi8l9afSkJtw+Of7hc4APyhlOw
//        HPiGdi2H3MUVnoHg23thzo7feHtzS+7Id+vBRQ7HKZrhHVpvnx2EsgnurZ1+p0ug
//        xCLpG4KBsmoD4yiDUtcBAGG5aG2El709G94cQ9uj2DXN2rnwL+VrR5GQOHqFeNUI
//        rTKUG4lwCPcvPOvnpdYj2jv4oj4uO2cbmgbZThcl4KdHK/Eo/jHr0UOhiT5J9ocm
//        RKryYYjEXE/t57tR2e0Rsel74fTmcgNygiixMjKDC1cmqX4R+g67m1gfR+/+SXR8
//        S9f9VzcwugcTnxIhke3TRta53QgfPNLOidpMM1tLwQKBgQC9faOxEVJ2KTQaAAMw
//        Nx8bBxhev5mifi+f8d14ERkG7XFb4SzPeUY29oB0KVxDyBwR8rgNars+GpUnquZv
//        91PVs5fYD3W+HwtOD/UOL0z3UtKnNI8nvtK08ru0PFDVzwzqEapy8dLkmbG556GP
//        HZ5WVn+8QeTX7GqbSU3xtPp21QKBgQDMJpTMzneQ+GrupU1lzdlD8GKF2RbsZ0Ui
//        rtIx4UYgIQV0lbvPhneJrGy16woOBUZ7jkCEDXKqofGumwCVfhpjjYzIqPfZzXaa
//        t5a6l2cLuwt0JnjluwqmIfWf1z+GdqCxgqUwdUgzxcPmzxcHwOCX1YFQQ8WONd6s
//        Id9DfAFjwQKBgQCLsKhQq11oAD4JgMLY83m52gQsLQEcWfvP5GSI08noYnhz7E61
//        cEjD0fqmJ6t9yHJxBMaMFYeNY9lbEdCo7+JcovWocNUy3/3cgUT9PP93QBZM7yEt
//        gq4geOTJHMHWrLlvgLBv5je7EFaFnu1p7MLCESg/ZzBFwWJhsauFKQ6PNQKBgFDc
//        PzfX15f+LSyVINDf9dxpDD0DvYapaMLSB8Nl/Qagza5d2GPcWOCZAP4VOIhRIpex
//        wnALe42GU1nbXyHXLtCbslWQR4tnTED/0p3ZdiE5VtIMovorWY5wCP/km+7Acemd
//        W5yT96M6A9wZzn9tsAezs2J9VXR8ddQsHmh2Z36BAoGBAIkFBge0QbWZGYCr3uk9
//        K0AhZUekGSzhakqp60XQs5kw8zb+TllCRxtYsQlyaHp1M8AH3Di/Uw+EhBt6h4Uw
//        fAPCZRg8vdG8Hp26PwXxybZ/M9u7NaKJ0BT4AwKKtZTUxZVxz/kPhdHT+MpoQqJf
//        JuzuwXVAAcl1GME2OiqkZhww
//        -----END PRIVATE KEY-----
//    """.trimIndent()
//    val keyId = KeyService.getService().importKey(keyStr)
//    println(keyId)
    val key = KeyService.getService()
        .export("324ef0e4b3344f1cbba4d484fa71a186", id.walt.services.key.KeyFormat.PEM, exportKeyType = KeyType.PRIVATE)
    println(key)
    val did = DidService.create(DidMethod.web, "324ef0e4b3344f1cbba4d484fa71a186", DidWebCreateOptions("walt.id", ".well-known"))
    println(did)
}
