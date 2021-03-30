package org.letstrust.crypto.keystore

import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId
import org.letstrust.services.key.Keys

interface KeyStore {
    fun store(key: Key)
    fun load(alias: String): Key
    fun addAlias(keyId: KeyId, alias: String)
    fun delete(alias: String)
    fun listKeys(): List<Keys>

    // OLD
    fun getKeyId(keyId: String): String?
    fun saveKeyPair(keys: Keys)
   // fun loadKeyPair(keyId: String): Keys?
    fun addAlias(keyId: String, alias: String)
}
