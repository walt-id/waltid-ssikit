package org.letstrust.services.keystore

import id.walt.servicematrix.BaseService
import id.walt.servicematrix.ServiceProvider
import id.walt.servicematrix.ServiceRegistry
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId

abstract class KeyStoreService : BaseService() {
    override val implementation get() = ServiceRegistry.getService<KeyStoreService>()

    open fun store(key: Key): Unit = implementation.store(key)
    open fun load(alias: String): Key = implementation.load(alias)
    open fun addAlias(keyId: KeyId, alias: String): Unit = implementation.addAlias(keyId, alias)
    open fun delete(alias: String): Unit = implementation.delete(alias)
    open fun listKeys(): List<Key> = implementation.listKeys()

    // OLD
    open fun getKeyId(keyId: String): String? = implementation.getKeyId(keyId)
    // fun saveKeyPair(keys: Keys)
    // fun loadKeyPair(keyId: String): Keys?
    //fun addAlias(keyId: String, alias: String)

    companion object : ServiceProvider {
        override fun getService() = object : KeyStoreService() {}
    }
}
