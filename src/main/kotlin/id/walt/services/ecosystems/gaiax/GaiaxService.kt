package id.walt.services.ecosystems.gaiax

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

abstract class GaiaxService : WaltIdService() {
    override val implementation get() = serviceImplementation<GaiaxService>()

    open fun generateGaiaxComplianceCredential(selfDescription: String): String =
        implementation.generateGaiaxComplianceCredential(selfDescription)

    companion object : ServiceProvider {
        override fun getService() = object : GaiaxService() {}
        override fun defaultImplementation() = WaltIdGaiaxService()
    }
}

class WaltIdGaiaxService : GaiaxService() {

    override fun generateGaiaxComplianceCredential(selfDescription: String): String {
        val complianceCredential = runBlocking {
            HttpClient(CIO).post("https://compliance.lab.gaia-x.eu/v2206/api/sign") {
                contentType(ContentType.Application.Json)
                setBody(selfDescription)
            }.bodyAsText()
        }

        return complianceCredential
    }

}
