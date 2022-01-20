package id.walt.services.did

import com.beust.klaxon.Klaxon
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class DidWebTest : StringSpec({

    "create did:web RSA" {
        createAndTestDidWeb(KeyAlgorithm.RSA)
    }

    "create did:web Secp256k1" {
        createAndTestDidWeb(KeyAlgorithm.ECDSA_Secp256k1)
    }

    "create did:web Ed25519" {
        createAndTestDidWeb(KeyAlgorithm.EdDSA_Ed25519)
    }

    "create did:web RSA with Options" {
        val options = DidService.DidWebOptions("example.com", "user-id-1234")
        createAndTestDidWeb(KeyAlgorithm.RSA, options)
    }

    "resolve did:web RSA".config(enabled = false) {
        val resolvedDid = DidService.resolve("did:web:example.com:user-id-1234")
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    "resolve did:web Secp256k1".config(enabled = false)  {
        println("hey Secp256k1")
    }

    "resolve did:web Ed25519".config(enabled = false)  {
        println("hey Ed25519")
    }
}) {

    override fun beforeSpec(spec: Spec) {
        super.beforeSpec(spec)

        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }
}

fun createAndTestDidWeb(keyAlgorith: KeyAlgorithm, options: DidService.DidWebOptions? = null): String {
    val keyId = KeyService.getService().generate(keyAlgorith).id
    val did = DidService.create(DidMethod.web, keyId, options)
    println(did)
    val didUrl = DidUrl.from(did)
    did shouldBe didUrl.did
    "web" shouldBe didUrl.method
    options?.let {
        did shouldContain "${options.domain!!}:${options.path}"
    } ?: run {
        did shouldContain "walt.id"
    }
    return did
}