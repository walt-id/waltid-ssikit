package org.letstrust.service2.services

import org.letstrust.service2.BaseService
import org.letstrust.service2.ServiceProvider
import org.letstrust.service2.ServiceRegistry

open class DidServiceDefault : DidService() {
    override fun import() = println("DID Import")
    override fun export() = println("DID Export")
}

abstract class DidService : BaseService() {
    override val implementation get() = ServiceRegistry.getService<DidService>()

    open fun import(): Unit = implementation.import()
    open fun export(): Unit = implementation.export()

    companion object : ServiceProvider {
        override fun getService() = object : DidService() {}
    }
}
