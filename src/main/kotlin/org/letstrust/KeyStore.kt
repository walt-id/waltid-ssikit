package org.letstrust

interface KeyStore {
    fun getKeyId(keyId: String): String?
    fun saveKeyPair(keys: Keys)
    fun listkeys(): List<Keys>
    fun loadKeyPair(keyId: String): Keys?
    fun deleteKeyPair(keyId: String)
    fun addAlias(keyId: String, identifier: String)
}
