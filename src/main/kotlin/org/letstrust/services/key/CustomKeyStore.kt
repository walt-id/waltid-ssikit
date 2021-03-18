package org.letstrust.services.key

class CustomKeyStore : KeyStore {
    override fun getKeyId(keyId: String): String? {
        TODO("Not yet implemented")
    }

    override fun saveKeyPair(keys: Keys) {
        TODO("Not yet implemented")
    }

    override fun listKeys(): List<Keys> {
        TODO("Not yet implemented")
    }

    override fun loadKeyPair(keyId: String): Keys? {
        TODO("Not yet implemented")
    }

    override fun deleteKeyPair(keyId: String) {
        TODO("Not yet implemented")
    }

    override fun addAlias(keyId: String, alias: String) {
        TODO("Not yet implemented")
    }
}
