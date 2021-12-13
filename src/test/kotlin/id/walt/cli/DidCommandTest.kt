package id.walt.cli

import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyAlgorithm.*
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File


class DidCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    beforeTest {
        File("test-dest.json").delete()
    }

    afterTest {
        File("test-dest.json").delete()
    }

    "Import did" {
        val testDid = "did:key:z6MkhjYgRWxZzr1suuVB2Skqym8HPEiRzrMW1W2KKKariqSz"
        ImportDidCommand().parse(listOf(testDid))

        val newDid = DidService.load(testDid)
        println("New DID: ${newDid.id}")
        newDid.id shouldBe testDid

        val key = KeyService.getService().load(testDid)
        println(key.keyId)

        key.keyId.id.removePrefix("did:key:") shouldBe "${testDid.removePrefix("did:key:")}#${testDid.removePrefix("did:key:")}"
    }

    "Create did:key Ed25519" {
        testDidKey(EdDSA_Ed25519)
    }

    "Create did:key Secp256k1" {
        testDidKey(ECDSA_Secp256k1)
    }

    "Create did:key RSA" {
        testDidKey(RSA)
    }

})

private fun testDidKey(algo: KeyAlgorithm) {
    val keyId = KeyService.getService().generate(algo)
    CreateDidCommand().parse(listOf("-m", "key", "-k", keyId.id, "test-dest.json"))
    File("test-dest.json").exists() shouldBe true
}
