package id.walt

import id.walt.credentials.w3c.W3CIssuer
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.services.vc.JwtCredentialService
import id.walt.services.vc.WaltIdJwtCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.getTemplate
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis


class Performance : StringSpec({
    ServiceMatrix("service-matrix.properties")
    val issuerWebDid: String = DidService.create(DidMethod.web)

    //val credentialService: JwtCredentialService = JwtCredentialService.getService()
    val signatory = Signatory.getService()
    val template = getTemplate("ebsi-attestation")

    template.issuer = W3CIssuer(issuerWebDid)
    template.credentialSubject!!.id = issuerWebDid // self signed

    val credOffer: String = template.toJson()

    /*
    "Issue 1" {
        val jobs = ArrayList<Job>()
        repeat(10000) {
            jobs.add(GlobalScope.launch {
                println("" + measureTimeMillis {
                    signatory.issue("VerifiableId", ProofConfig(issuerDid = issuerWebDid, proofType = ProofType.JWT))
                } + " ms")
            })
        }

        jobs.forEach { it.join() }
    }

    "Issue 2" {
        val jobs = ArrayList<Job>()

        measureTimeMillis {

            repeat(50000) {
                launch(Dispatchers.Default) {
                    signatory.issue("VerifiableId", ProofConfig(issuerDid = issuerWebDid, proofType = ProofType.JWT))
                }.let { jobs.add(it) }
            }

            jobs.forEach { it.join() }
        }.let { println("=> Full issuance: $it ms") }
    }

    "Issue 3" {
        val offers = ArrayList<String>().apply {
            repeat(10000) {
                add(credOffer)
            }
        }

        measureTimeMillis {
            offers
                .map { /*launch(Dispatchers.Default) {*/
                    measureTimeMillis {
                        signatory.issue("VerifiableId", ProofConfig(issuerDid = issuerWebDid, proofType = ProofType.JWT))
                    }.let { println("Single issuance: $it ms") }
                /*}*/}
                //.forEach { it.join() }
        }.let { println("=> Full issuance: $it ms") }
    }
     */
})
