package id.walt.signatory

import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class SignatoryServiceTest : StringSpec({
    ServiceMatrix("service-matrix.properties")
    val signatory = Signatory.getService()

    "test signatory issuing" {
        val vc = signatory.issue(
            "Europass", ProofConfig(
                issuerDid = DidService.listDids().first(),
                issuerVerificationMethod = "Ed25519Signature2018"
            )
        )

        println(vc)

        vc shouldContain "Europass"
        vc shouldContain "Université de Lille"
        vc shouldContain "MASTERS LAW, ECONOMICS AND MANAGEMENT"
    }
})
