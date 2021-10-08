package id.walt.services.jwt

import id.walt.crypto.WaltIdInMemoryProvider
import id.walt.servicematrix.ServiceProvider
import id.walt.services.key.InMemoryKeyService

class InMemoryJwtService : WaltIdJwtService() {
    override val keyService = InMemoryKeyService.getService()
    override val provider = WaltIdInMemoryProvider()

    companion object : ServiceProvider {
        val implementation = InMemoryJwtService()
        override fun getService() = implementation
    }
}