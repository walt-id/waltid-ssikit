package id.walt.services.vc

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService

open class RevocationClientService : WaltIdService() {
    override val implementation get() = serviceImplementation<RevocationClientService>()

    companion object : ServiceProvider {
        override fun getService() = object : RevocationClientService() {}
        override fun defaultImplementation() = WaltIdRevocationClientService()
    }
}

class WaltIdRevocationClientService : RevocationClientService() {

}
