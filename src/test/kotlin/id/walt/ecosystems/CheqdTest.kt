package id.walt.ecosystems

import id.walt.auditor.Auditor
import id.walt.auditor.policies.SignaturePolicy
import id.walt.credentials.w3c.W3CIssuer
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.keystore.KeyStoreService
import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class CheqdTest : StringSpec({

    ServiceMatrix("service-matrix.properties")

    var did: String? = null

    "Generating fresh did:cheqd" {
        println("Generating did:cheqd...")
        did = DidService.create(DidMethod.cheqd)
        println("Created did: $did")
    }

    var vc: String? = null

    "Create VC with did:cheqd" {
        vc = Signatory.getService().issue("VerifiableId", ProofConfig(did!!, did!!), issuer = W3CIssuer(did!!))
        println("Generated VC: $vc")
    }

    "Remove did:cheqd from keystore" {
        KeyStoreService.getService().delete(did!!)
        println("Deleted: $did")
    }

    /*"Import did:cheqd" {
        println("Importing: $did")
        DidService.importDidAndKeys(did!!)
    }*/

    "Test did:cheqd verification" {
        val verificationResult = Auditor.getService().verify(vc!!, listOf(SignaturePolicy()))

        verificationResult.policyResults.entries.forEach {
            if (it.value.isFailure) {
                println("ERROR at ${it.key}")
                it.value.errors.forEachIndexed { index, throwable ->
                    println("Error #$index: ${throwable.message}")
                    throwable.printStackTrace()
                }
            } else println("OK: ${it.key}")
        }
        verificationResult.result shouldBe true

    }

})
