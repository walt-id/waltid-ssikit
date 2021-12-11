package id.walt.cli

import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.script.test
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe


class DidCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    "did import" {
        val testDid = "did:key:z6MkhjYgRWxZzr1suuVB2Skqym8HPEiRzrMW1W2KKKariqSz"
        ImportDidCommand().parse(listOf(testDid))

        val newDid = DidService.load(testDid)
        println("New DID: ${newDid.id}")
        newDid.id shouldBe testDid

        val key = KeyService.getService().load(testDid)
        println(key.keyId)

        key.keyId.id shouldBe "${testDid.removePrefix("did:key:")}#${testDid.removePrefix("did:key:")}"
    }


})
