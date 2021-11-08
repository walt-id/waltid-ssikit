package id.walt.services.keystore

import id.walt.crypto.*
import id.walt.services.context.WaltContext
import id.walt.services.hkvstore.HKVKey
import mu.KotlinLogging

open class HKVKeyStoreService : KeyStoreService() {

    private val log = KotlinLogging.logger {}
    open val hkvStore
        get() = WaltContext.hkvStore // lazy load!

    //TODO: get key format from config
    private val KEY_FORMAT = KeyFormat.PEM
    private val KEYS_ROOT = HKVKey("keystore", "keys")
    private val ALIAS_ROOT = HKVKey("keystore", "alias")

    override fun listKeys(): List<Key> = hkvStore.listChildKeys(KEYS_ROOT)
        .map {
            load(it.name.substringBefore("."))
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
        val aliases = hkvStore.getAsString(HKVKey.combine(KEYS_ROOT, keyId.id, "aliases"))?.split("\n")?.plus(alias) ?: listOf(alias)
        hkvStore.put(HKVKey.combine(KEYS_ROOT, keyId.id, "aliases"), aliases.joinToString("\n"))
    }

    override fun store(key: Key) {
        log.debug { "Storing key \"${key.keyId}\"." }
        //hkvStore.put(HKVKey("keys", key.keyId.id), key.keyId.id)
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storePublicKey(key)
        storePrivateKeyWhenExisting(key)
    }

    override fun getKeyId(alias: String) = runCatching { hkvStore.getAsString(HKVKey.combine(ALIAS_ROOT, alias)) }.getOrNull()

    override fun delete(alias: String) {
        val keyId = getKeyId(alias)
        if(keyId.isNullOrEmpty())
            return
        val aliases = hkvStore.getAsString(HKVKey.combine(KEYS_ROOT, keyId, "aliases")) ?: ""
        aliases.split("\n").forEach { a -> hkvStore.delete(HKVKey.combine(ALIAS_ROOT, a), recursive = false) }
        hkvStore.delete(HKVKey.combine(KEYS_ROOT, keyId), recursive = true)
    }

    private fun storePublicKey(key: Key) =
        saveKeyData(
            key, "enc-pubkey",
            when (KEY_FORMAT) {
                KeyFormat.PEM -> key.getPublicKey().toPEM()
                else -> key.getPublicKey().toBase64()
            }.toByteArray()
        )

    private fun storePrivateKeyWhenExisting(key: Key) {
        if (key.keyPair != null && key.keyPair!!.private != null) {
            saveKeyData(
                key, "enc-privkey", when (KEY_FORMAT) {
                    KeyFormat.PEM -> key.keyPair!!.private.toPEM()
                    else -> key.keyPair!!.private.toBase64()
                }.toByteArray()
            )
        }
    }

    private fun storeKeyMetaData(key: Key) {
        saveKeyData(key, "meta", (key.algorithm.name + ";" + key.cryptoProvider.name).toByteArray())
    }

    private fun saveKeyData(key: Key, suffix: String, data: ByteArray): Unit =
        hkvStore.put(HKVKey.combine(KEYS_ROOT, key.keyId.id, suffix), data)

    private fun loadKey(keyId: String, suffix: String): ByteArray =
        hkvStore.getAsByteArray(HKVKey.combine(KEYS_ROOT, keyId, suffix))!!
}
