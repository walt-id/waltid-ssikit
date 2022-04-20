package id.walt.cli

import id.walt.common.readWhenContent
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyAlgorithm.*
import id.walt.crypto.KeyId
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readLines

class DidCommandTest : StringSpec({

    ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")

    beforeTest {
        File("test-dest.json").delete()
    }

    afterTest {
        File("test-dest.json").delete()
    }

    "1. Import did" {
        val testDid = "did:key:z6MkhjYgRWxZzr1suuVB2Skqym8HPEiRzrMW1W2KKKariqSz"
        ImportDidCommand().parse(listOf("-d", testDid))

        val newDid = DidService.load(testDid)
        println("New DID: ${newDid.id}")
        newDid.id shouldBe testDid

        val key = KeyService.getService().load(testDid)
        println(key.keyId)

        val keyIdWithoutPrefix = key.keyId.id.removePrefix("did:key:")
        val didWithoutPrefix = "${testDid.removePrefix("did:key:")}#${testDid.removePrefix("did:key:")}"
        keyIdWithoutPrefix shouldBe didWithoutPrefix
    }

    "2. Create did:key Ed25519" {
        testDidKey(EdDSA_Ed25519)
    }

    "3. Create did:key Secp256k1" {
        testDidKey(ECDSA_Secp256k1)
    }

    "4. Create did:key RSA" {
        testDidKey(RSA)
    }

    var rsaKeyId: KeyId? = null
    //var didWebRsa: String? = null
    var didKeyRsa: String? = null
    "5. Create did:web RSA" {
        rsaKeyId = KeyService.getService()
            .importKey(readWhenContent(Path.of("src/test/resources/key/rsa.pem")))

        CreateDidCommand().parse(listOf("-m", "web", "-k", rsaKeyId!!.id, "test-dest.json"))
        /*didWebRsa = Path.of("test-dest.json")
            .readLines()
            .first { it.trim().startsWith("\"did:web:walt.id") }
            .trim()
            .trim { it == '\"' }*/
    }

    /* TODO Can't check upload of did:web
    "6. Resolve did:web RSA" {
        println(didWebRsa!!)
        ResolveDidCommand().parse(listOf("-d", didWebRsa!!))
    }*/

    "6. Create RSA did:key" {
        CreateDidCommand().parse(listOf("-m", "key", "-k", rsaKeyId!!.id, "test-dest.json"))
        didKeyRsa = Path.of("test-dest.json")
             .readLines()
             .first { it.trim().startsWith("\"did:key:") }
             .trim()
             .trim { it == '\"' }
    }
    "7. Resolve RSA did:key" {
        println(didKeyRsa!!)
        ResolveDidCommand().parse(listOf("-d", didKeyRsa!!))
    }

    "8. delete did"{
        forAll(
            row(DidMethod.key, null),
            row(DidMethod.web, null),
            row(DidMethod.ebsi, null),
            row(DidMethod.key, KeyService.getService().generate(ECDSA_Secp256k1).id),
            row(DidMethod.key, KeyService.getService().generate(EdDSA_Ed25519).id),
            row(DidMethod.key, KeyService.getService().generate(RSA).id),
            row(DidMethod.web, KeyService.getService().generate(ECDSA_Secp256k1).id),
            row(DidMethod.web, KeyService.getService().generate(EdDSA_Ed25519).id),
            row(DidMethod.web, KeyService.getService().generate(RSA).id),
            row(DidMethod.ebsi, KeyService.getService().generate(ECDSA_Secp256k1).id),
            row(DidMethod.ebsi, KeyService.getService().generate(EdDSA_Ed25519).id),
            row(DidMethod.ebsi, KeyService.getService().generate(RSA).id),
        ){ method, key ->
            val did = DidService.create(method, key)
//            val ids = DidService.load(did).verificationMethod?.map { it.id }
            // delete
            DeleteDidCommand().parse(listOf("-d", did))
        }
    }
})

private fun testDidKey(algo: KeyAlgorithm) {
    val keyId = KeyService.getService().generate(algo)
    CreateDidCommand().parse(listOf("-m", "key", "-k", keyId.id, "test-dest.json"))
    File("test-dest.json").exists() shouldBe true
}
