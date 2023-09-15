package id.walt.services.key.import

import id.walt.crypto.KeyId
import id.walt.services.key.deriver.SunPublicKeyDeriver
import id.walt.services.keystore.KeyStoreService

interface KeyImportStrategy {
    fun import(keyStore: KeyStoreService): KeyId
}

abstract class KeyImportFactory {
    companion object {
        private val publicKeyDeriver = SunPublicKeyDeriver()
        fun create(keyString: String) = when (isPEM(keyString)) {
            true -> PemKeyImport(keyString, publicKeyDeriver)
            false -> JwkKeyImport(keyString)
        }

        private fun isPEM(keyString: String) = keyString.startsWith("-----")
    }
}
