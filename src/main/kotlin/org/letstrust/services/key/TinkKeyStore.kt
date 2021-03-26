package org.letstrust.services.key

import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyId
import java.io.File




object TinkKeyStore : KeyStoreBase() {
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
        println(keysetHandle)
        val metaData = loadMetaData(keyId)
        return Key(keyId, metaData.algorithm, metaData.cryptoProvider, keysetHandle)
    }

}
