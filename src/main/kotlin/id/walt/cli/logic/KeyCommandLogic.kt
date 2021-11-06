package id.walt.cli.logic

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.services.key.KeyFormat
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType

object KeyCommandLogic {

    private val keyService = KeyService.getService()


    fun genKey(algorithm: String): KeyId = when (algorithm) {
        "Ed25519" -> keyService.generate(KeyAlgorithm.EdDSA_Ed25519)
        "Secp256k1" -> keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        else -> throw IllegalArgumentException("Algorithm not supported")
    }

    fun import(keyStr: String) = keyService.importKey(keyStr)
    fun export(keyId: String, keyFormat: KeyFormat, exportKeyType: KeyType) = keyService.export(keyId, keyFormat, exportKeyType)
    fun listKeys() = keyService.listKeys()
}
