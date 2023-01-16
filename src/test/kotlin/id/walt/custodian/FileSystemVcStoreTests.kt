package id.walt.custodian

import id.walt.credentials.w3c.W3CCredentialSchema
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.servicematrix.ServiceMatrix
import id.walt.servicematrix.ServiceRegistry
import id.walt.services.vcstore.FileSystemVcStoreService
import id.walt.services.vcstore.VcStoreService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class FileSystemVcStoreTests : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    val custodian = Custodian.getService()
    ServiceRegistry.registerService<VcStoreService>(FileSystemVcStoreService())

    val vc = W3CCredentialBuilder().setCredentialSchema(W3CCredentialSchema("europass", "")).build()

    "1: Store credential" {
        custodian.storeCredential("my-test-europass", vc)

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
        retrievedVc shouldNotBe null
        println(retrievedVc!!.credentialSchema!!.id)
    }

    "3: List credentials" {
        custodian.listCredentials().map { it.id }.contains(vc.id) shouldBe true
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
