package org.letstrust.services.key

import org.letstrust.KeyId
import org.letstrust.crypto.Key

interface KeyStore {
    fun getKeyId(keyId: String): String?
    fun saveKeyPair(keys: Keys)
    fun listKeys(): List<Keys>
    fun loadKeyPair(keyId: String): Keys?
    fun deleteKeyPair(keyId: String)
    fun addAlias(keyId: String, alias: String)
    fun store(key: Key)
    fun load(keyId: KeyId): Key
}
