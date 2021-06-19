package org.letstrust.service2

import org.letstrust.service2.services.*

fun main() {
    println("> Registering services...")

    val serviceMatrix = ServiceMatrix("service-matrix.properties")

    //ServiceRegistry.registerService<VCService>(VcServiceDefault())
    //ServiceRegistry.registerService<DidService>(DidServiceDefault())

    println("> Getting service...")
    val vcService = VCService.getService()
    val didService = DidService.getService()

    println("> VC Functions:")
    vcService.import()
    vcService.export()

    //println("Setting other class...")
    //ServiceRegistry.registerService<VCService>(CustomVCService())

    //val instance: BaseService = Class.forName("org.letstrust.service2.services.CustomVCService").getDeclaredConstructor().newInstance() as BaseService
    //val service: Class<BaseService> = Class.forName("org.letstrust.service2.services.VCService") as Class<BaseService>
    //ServiceRegistry.registerService(instance, service.kotlin)

    println("> VC Functions:")
    vcService.import()
    vcService.export()

    println("> DID Functions:")
    didService.import()
    didService.export()
}
