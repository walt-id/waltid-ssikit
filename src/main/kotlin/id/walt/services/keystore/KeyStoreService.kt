package id.walt.services.keystore

import id.walt.crypto.Key
import id.walt.crypto.KeyId
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.WaltIdService

enum class KeyType {
    PUBLIC,
    PRIVATE
}

abstract class KeyStoreService : WaltIdService() {
    override val implementation get() = ServiceRegistry.getService<KeyStoreService>()

    open fun store(key: Key): Unit = implementation.store(key)
    open fun load(alias: String, keyType: KeyType = KeyType.PUBLIC): Key = implementation.load(alias, keyType)
    open fun addAlias(keyId: KeyId, alias: String): Unit = implementation.addAlias(keyId, alias)
    open fun delete(alias: String): Unit = implementation.delete(alias)
    open fun listKeys(): List<Key> = implementation.listKeys()

    // OLD
    open fun getKeyId(alias: String): String? = implementation.getKeyId(alias)
    // fun saveKeyPair(keys: Keys)
    // fun loadKeyPair(keyId: String): Keys?
    //fun addAlias(keyId: String, alias: String)
}
