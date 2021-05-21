package org.letstrust.service2.services

import org.letstrust.service2.LetstrustService
import org.letstrust.service2.LetstrustServiceProvider
import org.letstrust.service2.LetstrustServiceRegistry

open class LetstrustVCService : VCService() {
    override fun import() = println("VC Import")
    override fun export() = println("VC Export")
}

abstract class VCService : LetstrustService() {
    override val implementation get() = LetstrustServiceRegistry.getService<VCService>()

    open fun import(): Unit = implementation.import()
    open fun export(): Unit = implementation.export()

    companion object : LetstrustServiceProvider {
        override fun getService() = object : VCService() {}
    }
}
