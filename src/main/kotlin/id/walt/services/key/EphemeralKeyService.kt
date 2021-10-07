package id.walt.services.key

import id.walt.services.crypto.CryptoService
import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService
import id.walt.services.keystore.KeyStoreService

class EphemeralKeyService : WaltIdKeyService() {
    override val keyStore: KeyStoreService = InMemoryKeyStoreService()
    override val cryptoService: CryptoService = SunCryptoService().let { it.setKeyStore(keyStore) ; it }
}