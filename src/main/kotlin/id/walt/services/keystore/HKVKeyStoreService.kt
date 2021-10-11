package id.walt.services.keystore

import id.walt.crypto.*
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
import mu.KotlinLogging

open class HKVKeyStoreService : KeyStoreService() {

    private val log = KotlinLogging.logger {}
    open val hkvStore
        get() = HKVStoreService.getService() // lazy load!

    //TODO: get key format from config
    private val KEY_FORMAT = KeyFormat.PEM

    override fun listKeys(): List<Key> = hkvStore.listChildKeys(HKVKey("keys"))
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

    override fun addAlias(keyId: KeyId, alias: String) = hkvStore.put(HKVKey("keys", "alias", alias), keyId.id)

    override fun store(key: Key) {
        log.debug { "Storing key \"${key.keyId}\"." }
        hkvStore.put(HKVKey("keys", key.keyId.id), key.keyId.id)
        addAlias(key.keyId, key.keyId.id)
        storeKeyMetaData(key)
        storePublicKey(key)
        storePrivateKeyWhenExisting(key)
    }

    override fun getKeyId(alias: String) = runCatching { hkvStore.getAsString(HKVKey("keys", "alias", alias)) }.getOrNull()

    override fun delete(alias: String) {
        hkvStore.delete(HKVKey("keys", alias), recursive = true)
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
        hkvStore.put(HKVKey("keys", key.keyId.id, suffix), data)

    private fun loadKey(keyId: String, suffix: String): ByteArray =
        hkvStore.getAsByteArray(HKVKey("keys", keyId, suffix))!!
}
