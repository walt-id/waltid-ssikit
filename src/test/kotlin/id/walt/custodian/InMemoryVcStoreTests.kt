package id.walt.custodian

import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.vcstore.InMemoryVcStoreService
import id.walt.services.vcstore.VcStoreService
import id.walt.test.RESOURCES_PATH
import id.walt.vclib.vclist.Europass
import id.walt.vclib.vclist.VerifiableAttestation
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe

class InMemoryVcStoreTests : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val custodian = Custodian.getService()
    ServiceRegistry.registerService<VcStoreService>(InMemoryVcStoreService())

    val vc = Europass.template!!.invoke()

    "1: Store credentials" {
        custodian.storeCredential("my-test-europass", vc)
        custodian.storeCredential("my-test-va", VerifiableAttestation.template!!.invoke())

        custodian.listCredentialIds().size shouldBeGreaterThanOrEqual 1
    }

    "2: List credential ids" {
        custodian.listCredentialIds().forEach {
            println(it)
        }
    }

    "3: Retrieve credential" {
        val retrievedVc = custodian.getCredential("my-test-europass")
        println(retrievedVc)
        retrievedVc as Europass
        println(retrievedVc.credentialSchema!!.id)
    }

    "3: List credentials" {
        custodian.listCredentials().contains(vc) shouldBe true
    }

    "4: Delete credentials" {
        custodian.listCredentialIds().forEach {
            custodian.deleteCredential(it)
        }

        custodian.listCredentialIds().forEach {
            println("! Remaining credential id: $it")
        }

        custodian.listCredentialIds().size shouldBe 0
        custodian.listCredentials().size shouldBe 0
    }
})
