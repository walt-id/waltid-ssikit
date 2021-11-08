package id.walt.services.key

import id.walt.servicematrix.ServiceProvider
import id.walt.services.crypto.SunCryptoService
import id.walt.services.keystore.InMemoryKeyStoreService

class InMemoryKeyService : WaltIdKeyService() {

    override val keyStore = InMemoryKeyStoreService()
    override val cryptoService = SunCryptoService().let { it.setKeyStore(keyStore) ; it }

    companion object : ServiceProvider {
        val implementation = InMemoryKeyService()
        override fun getService() = implementation
    }
}
