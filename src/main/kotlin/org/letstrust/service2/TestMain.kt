package org.letstrust.service2

import org.letstrust.service2.services.*

fun main() {
    println("Registering services...")
    ServiceRegistry.registerService<VCService>(VcServiceInternal())
    ServiceRegistry.registerService<DidService>(DidServiceInternal())

    println("Getting service...")
    val vcService = VCService.getService()
    val didService = DidService.getService()

    println("Functions...")
    vcService.import()
    vcService.export()

    println("Setting other class...")
    ServiceRegistry.registerService<VCService>(CustomVCService())

    println("VC Import...")
    vcService.import()
    vcService.export()

    println("DID Import...")
    didService.import()
    didService.export()
}
