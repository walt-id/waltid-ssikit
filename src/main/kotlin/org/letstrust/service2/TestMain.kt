package org.letstrust.service2

import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import org.letstrust.services.vc.CustomVCService
import org.letstrust.services.vc.VCService

fun main() {
    println("> Registering services...")

    ServiceMatrix("service-matrix.properties")

    //ServiceRegistry.registerService<VCService>(LetstrustVCService())
    /*ServiceRegistry.registerService<VCService>(LetstrustVCService())
    ServiceRegistry.registerService<DidService>(DidServiceDefault())*/

    println("> Getting service...")
    val vcService = VCService.getService()

    /*val didService = DidService.getService()*/

    println("> VC Functions:")
    vcService.listTemplates().forEach { println(it) }
    vcService.listVCs().forEach { println(it) }

    println("Setting other class...")
    ServiceRegistry.registerService<VCService>(CustomVCService())

    //val instance: BaseService = Class.forName("org.letstrust.services.CustomLetstrustVCService").getDeclaredConstructor().newInstance() as BaseService
    //val service: Class<BaseService> = Class.forName("org.letstrust.service2.services.VCService") as Class<BaseService>
    //ServiceRegistry.registerService(instance, service.kotlin)

    println("> VC Functions:")
    vcService.listTemplates().forEach { println(it) }
    vcService.listVCs().forEach { println(it) }

    /*println("> DID Functions:")
    didService.import()
    didService.export()*/
}
