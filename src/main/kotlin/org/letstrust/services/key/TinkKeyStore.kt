package org.letstrust.services.key

import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import org.letstrust.CryptoProvider
import org.letstrust.KeyAlgorithm
import org.letstrust.KeyId
import org.letstrust.crypto.Key
import java.io.File

object TinkKeyStore : KeyStore {
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

    override fun store(key: Key) {
        CleartextKeysetHandle.write(key.keysetHandle, JsonKeysetWriter.withFile(File(key.keyId.id + ".tink")))
    }

    override fun load(keyId: KeyId): Key {
        val keysetHandle =  CleartextKeysetHandle.read(JsonKeysetReader.withFile(File(keyId.id + ".tink")))
        return Key(keyId, KeyAlgorithm.Secp256k1, CryptoProvider.TINK, keysetHandle)
    }
}
