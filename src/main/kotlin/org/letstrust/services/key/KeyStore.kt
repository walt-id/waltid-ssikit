package org.letstrust.services.key

interface KeyStore {
    fun getKeyId(keyId: String): String?
    fun saveKeyPair(keys: Keys)
    fun listKeys(): List<Keys>
    fun loadKeyPair(keyId: String): Keys?
    fun deleteKeyPair(keyId: String)
    fun addAlias(keyId: String, alias: String)
}
