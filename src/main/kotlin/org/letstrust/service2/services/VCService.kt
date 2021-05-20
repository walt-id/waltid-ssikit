package org.letstrust.service2.services

import org.letstrust.service2.LetstrustService
import org.letstrust.service2.LetstrustServiceProvider
import org.letstrust.service2.LetstrustServiceRegistry

open class LetstrustVCService : VCService() {
    override fun import() = println("VC Import")
    override fun export() = println("VC Export")
}

class VCServiceWrapper : VCService() {
    override fun import() = implementation.import()
    override fun export() = implementation.export()
}

abstract class VCService : LetstrustService() {
    override val implementation: VCService
        get() = LetstrustServiceRegistry.getService<VCService>() as VCService

    abstract fun import()
    abstract fun export()

    companion object : LetstrustServiceProvider {
        override fun getService() = VCServiceWrapper()
    }
}
