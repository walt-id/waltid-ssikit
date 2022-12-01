package id.walt

import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import java.io.File

fun main() {
    ServiceMatrix("service-matrix.properties")
    val did = DidService.importDidFromFile(File("did-cheqd.json"))

    println("DID imported: $did")
}
