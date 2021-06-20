package org.letstrust.service2

import org.letstrust.services.vc.CustomVCService
import org.letstrust.services.vc.VCService

fun main() {
    println("> Registering services...")

    //val serviceMatrix = ServiceMatrix("service-matrix.properties")

    ServiceRegistry.registerService<VCService>(CustomVCService())
    /*ServiceRegistry.registerService<VCService>(LetstrustVCService())
    ServiceRegistry.registerService<DidService>(DidServiceDefault())*/

    println("> Getting service...")
    val vcService = VCService.getService()

    println("Listing templates")
    vcService.listTemplates().forEach {
        println(it)
    }

    /*val didService = DidService.getService()*/

    /*println("> VC Functions:")
    vcService.import()
    vcService.export()*/

    /*println("Setting other class...")
    ServiceRegistry.registerService<VCService>(CustomLetstrustVCService())*/

    //val instance: BaseService = Class.forName("org.letstrust.services.CustomLetstrustVCService").getDeclaredConstructor().newInstance() as BaseService
    //val service: Class<BaseService> = Class.forName("org.letstrust.service2.services.VCService") as Class<BaseService>
    //ServiceRegistry.registerService(instance, service.kotlin)

    /*println("> VC Functions:")
    vcService.import()
    vcService.export()

    println("> DID Functions:")
    didService.import()
    didService.export()*/
}
