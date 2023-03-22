package id.walt.services.ecosystems.gaiax

import id.walt.servicematrix.ServiceProvider
import id.walt.services.WaltIdService
import id.walt.services.WaltIdServices.httpNoAuth
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

    val testCred = """
    {
        "@context": [
            "https://www.w3.org/2018/credentials/v1"
        ],
        "type": [
            "VerifiableCredential",
            "ParticipantCredential"
        ],
        "id": "https://catalogue.gaia-x.eu/credentials/ParticipantCredential/1664281027011",
        "issuer": "did:web:compliance.lab.gaia-x.eu",
        "issuanceDate": "2022-09-27T12:17:07.012Z",
        "credentialSubject": {
            "id": "did:web:compliance.gaia-x.eu",
            "hash": "44166d1e997147db7fcbb3a8d201af9bf830a291b1e8837954017f5440785ede"
        },
        "proof": {
            "type": "JsonWebSignature2020",
            "created": "2022-09-27T12:17:07.012Z",
            "proofPurpose": "assertionMethod",
            "jws": "eyJhbGciOiJQUzI1NiIsImI2NCI6ZmFsc2UsImNyaXQiOlsiYjY0Il19..dcwKNbNtfo8l-bMqz8NUQh2ljoG3HiGY7pQ3y1bQG0oUoAxWWodzR2LOVuXGVtd5VTBNH2XMl1y6-t4IF-SIwviZt6a_6nLkzhV5rVrtUHNAo-88o7ejYqrki4xxXChp55OsZ5hCmWePkljjDtKx2A1qIB_2PsH3lU7WrcvtbpF1gMAPQiCOQE2Wdl2Hw2t7yTE8XejYdJC0Ggz_QMKuqq_wHJ_JJ6kHbEWmQffsFRUxAGDXDPAfMvi7X71d6wqGT7pbfkcUYEbX2d8z74ceOp41LTMMRfArJzLDxeg8Db8uwlKZrp4QYHH6vMB0eabICtctCPeYTw5Zh8LmSkSaZA",
            "verificationMethod": "did:web:compliance.lab.gaia-x.eu"
        }
    }
    """.trimIndent()

    override fun generateGaiaxComplianceCredential(selfDescription: String): String {

        val complianceCredential = runCatching {

            val complianceCredential = runBlocking {
                val req = httpNoAuth.post("https://compliance.lab.gaia-x.eu/v2206/api/sign") {
                    contentType(ContentType.Application.Json)
                    setBody(selfDescription)
                }

                if (req.status !in listOf(HttpStatusCode.OK, HttpStatusCode.Accepted, HttpStatusCode.Created)) {
                    throw IllegalStateException("could not send request")
                }

                req.bodyAsText()
            }

            complianceCredential
        }.getOrElse { testCred }

        return complianceCredential
    }

}
