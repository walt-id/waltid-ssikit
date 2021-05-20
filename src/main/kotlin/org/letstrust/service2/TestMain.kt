package org.letstrust.service2

import org.letstrust.service2.services.CustomVCService
import org.letstrust.service2.services.LetstrustVCService
import org.letstrust.service2.services.VCService

fun main() {
    println("Registering services...")
    LetstrustServiceRegistry.registerService<VCService>(LetstrustVCService())

    println("Getting service...")
    val vcService = VCService.getService()

    println("Functions...")
    vcService.import()
    vcService.export()

    println("Setting other class...")
    LetstrustServiceRegistry.registerService<VCService>(CustomVCService())

    println("Import...")
    vcService.import()
    vcService.export()
}
