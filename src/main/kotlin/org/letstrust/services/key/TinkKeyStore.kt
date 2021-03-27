package org.letstrust.services.key

import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import org.letstrust.KeyAlgorithm
import org.letstrust.LetsTrustServices
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
        CleartextKeysetHandle.write(key.keysetHandle, JsonKeysetWriter.withFile(File("${LetsTrustServices.keyDir}/${key.keyId.id}.tink")))
    }

    override fun load(keyId: KeyId): Key {
        val keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withFile(File("${LetsTrustServices.keyDir}/${keyId.id}.tink")))
        val algorithm = when (keysetHandle.keysetInfo.getKeyInfo(0).typeUrl) {
            "type.googleapis.com/google.crypto.tink.Ed25519PrivateKey" -> KeyAlgorithm.Ed25519
            "type.googleapis.com/google.crypto.tink.EcdsaPrivateKey" -> KeyAlgorithm.Secp256k1
            else  -> throw Exception("Could not determine KeyAlgorithm")
        }
        println(keysetHandle)
        val metaData = loadMetaData(keyId)
        return Key(keyId, algorithm, metaData.cryptoProvider, keysetHandle)
    }

    override fun addAlias(keyId: KeyId, alias: String) {
        TODO("Not yet implemented")
    }

}
