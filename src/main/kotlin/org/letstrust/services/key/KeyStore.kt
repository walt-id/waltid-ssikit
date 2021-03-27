package org.letstrust.services.key

import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId

interface KeyStore {
    fun store(key: Key)
    fun load(keyId: KeyId): Key
    fun addAlias(keyId: KeyId, alias: String)

    // OLD
    fun getKeyId(keyId: String): String?
    fun saveKeyPair(keys: Keys)
    fun listKeys(): List<Keys>
    fun loadKeyPair(keyId: String): Keys?
    fun deleteKeyPair(keyId: String)
    fun addAlias(keyId: String, alias: String)
}

//abstract class KeyStoreBase : KeyStore {
//
//    fun loadMetaData(keyId: KeyId): KeyMetaData {
//        // TODO: implement
//        return KeyMetaData(KeyAlgorithm.Secp256k1, CryptoProvider.TINK)
//    }
//}
