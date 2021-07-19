package org.letstrust.crypto.keystore

import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId

interface KeyStore {
    fun store(key: Key)
    fun load(alias: String, loadPrivate: Boolean = false): Key
    fun addAlias(keyId: KeyId, alias: String)
    fun delete(alias: String)
    fun listKeys(): List<Key>

    // OLD
    fun getKeyId(keyId: String): String?
    // fun saveKeyPair(keys: Keys)
    // fun loadKeyPair(keyId: String): Keys?
    //fun addAlias(keyId: String, alias: String)
}
