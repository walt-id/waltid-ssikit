package id.walt.services.did

import id.walt.crypto.KeyId
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.network.sockets.*
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.readText

class DidCheqdTest : StringSpec({
    ServiceMatrix("service-matrix.properties")

    "Test DidService DID CHEQD resolving" {
        val did = DidService.resolve("did:cheqd:mainnet:Ps1ysXP2Ae6GBfxNhNQNKN")
        println(did.id)
        did.id shouldBe "did:cheqd:mainnet:Ps1ysXP2Ae6GBfxNhNQNKN"
    }
    var didCheqdKeyId: KeyId? = null
    "Test did:cheqd key importing" {
        val keyStr = Path("src/test/resources/key/cheqd.jwk.json").readText()

        KeyService.getService().run {
            delete("259282edee7858a54cf59ca04bca1a37cc00cf332c77254b3f98828afc8acdbe")
            didCheqdKeyId = importKey(keyStr)
        }

        didCheqdKeyId shouldBe KeyId("259282edee7858a54cf59ca04bca1a37cc00cf332c77254b3f98828afc8acdbe")
    }
    "Import did:cheqd" {
        didCheqdKeyId ?: throw IllegalStateException("did:cheqd key was not imported in step 3!")

        val did = DidService.importDidFromFile(File("src/test/resources/dids/did-cheqd.json"))

        DidService.setKeyIdForDid(did, didCheqdKeyId!!.id)

        println("DID imported: $did")
    }

    "Register and load did:cheqd" {
        try {
            val did = DidService.create(DidMethod.cheqd)
            println("Created: $did")

            val loadedDid = DidService.load(did)
            println("Loaded: $loadedDid")
        } catch (e: SocketTimeoutException) {
            println("Timed out: (msg ${e.message})")
        }
    }
})

