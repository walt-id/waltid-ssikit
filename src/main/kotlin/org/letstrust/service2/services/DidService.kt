package org.letstrust.service2.services

import org.letstrust.service2.BaseService
import org.letstrust.service2.ServiceProvider
import org.letstrust.service2.ServiceRegistry

open class VcServiceInternal : VCService() {
    override fun import() = println("VC Import")
    override fun export() = println("VC Export")
}

abstract class VCService : BaseService() {
    override val implementation get() = ServiceRegistry.getService<VCService>()

    open fun import(): Unit = implementation.import()
    open fun export(): Unit = implementation.export()

    companion object : ServiceProvider {
        override fun getService() = object : VCService() {}
    }
}
