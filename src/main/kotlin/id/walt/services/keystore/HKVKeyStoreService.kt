package id.walt.services.keystore

import id.walt.crypto.*
import id.walt.services.context.ContextManager
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

        val publicPart = if (keyType == KeyType.PUBLIC) loadKey(keyId, "enc-pubkey").decodeToString() else null
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
