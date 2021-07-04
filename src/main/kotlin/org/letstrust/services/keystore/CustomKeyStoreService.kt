package org.letstrust.services.keystore

import org.letstrust.CryptoProvider
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.crypto.KeyId
import org.letstrust.crypto.PrivateKeyHandle
import java.security.KeyPair
import java.security.PublicKey


class CustomKeyStoreService : KeyStoreService() {

    fun generate() = Unit

    override fun getKeyId(keyId: String): String = TODO("Not yet implemented")

    override fun listKeys(): List<Key> = TODO("Not yet implemented")

    override fun delete(alias: String): Unit = TODO("Not yet implemented")

    override fun store(key: Key): Unit = TODO("Not yet implemented")

    override fun load(alias: String): Key {
        val publicKey: PublicKey? = null // TODO: load public key
        // The private key handle does not contain the privyte key material. It is only
        // used as reference to the private in the external key store.
        val privateKey = PrivateKeyHandle(KeyId(alias))
        return Key(KeyId(alias), KeyAlgorithm.EdDSA_Ed25519, CryptoProvider.CUSTOM, KeyPair(publicKey, privateKey))
    }

    override fun addAlias(keyId: KeyId, alias: String): Unit = TODO("Not yet implemented")
}
