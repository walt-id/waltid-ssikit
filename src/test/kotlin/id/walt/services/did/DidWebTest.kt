package id.walt.services.did

import com.beust.klaxon.Klaxon
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.LdVerificationKeyType.*
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.model.DidWeb
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.matchers.shouldBe
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class)
class DidWebTest : StringSpec({

    "create did:web RSA" {
        val did = createAndTestDidWeb(KeyAlgorithm.RSA)
        val didDoc = DidService.load(did)
        println(didDoc.encodePretty())
        didDoc.verificationMethod!![0].type shouldBe RsaVerificationKey2018.name
    }

    "create did:web Secp256k1" {
        val did = createAndTestDidWeb(KeyAlgorithm.ECDSA_Secp256k1)
        val didDoc = DidService.load(did)
        println(didDoc.encodePretty())
        didDoc.verificationMethod!![0].type shouldBe EcdsaSecp256k1VerificationKey2019.name
    }

    "create did:web Ed25519" {
        val did = createAndTestDidWeb(KeyAlgorithm.EdDSA_Ed25519)
        val didDoc = DidService.load(did)
        println(didDoc.encodePretty())
        didDoc.verificationMethod!![0].type shouldBe Ed25519VerificationKey2019.name
    }

    "create did:web custom domain" {
        val options = DidService.DidWebOptions("example.com")
        val keyId = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id
        val did = DidService.create(DidMethod.web, keyId, options)
        did shouldBe "did:web:example.com"
        val didDoc = DidService.load(did)
        println(didDoc.encodePretty())
    }

    "create did:web custom domain and path" {
        val options = DidService.DidWebOptions("example.com", "api/users/1234")
        val did = createAndTestDidWeb(KeyAlgorithm.RSA, options)
        did shouldBe "did:web:example.com:api:users:1234"
        val didDoc = DidService.load(did)
        println(didDoc.encodePretty())
    }

    "resolve did:web RSA".config(enabled = false) {
        val resolvedDid = DidService.resolve("did:web:example.com:user-id-1234")
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    "resolve did:web Secp256k1".config(enabled = false) {
        println("hey Secp256k1")
    }

    "resolve did:web Ed25519".config(enabled = false)  {
        println("hey Ed25519")
    }

    "resolve did:web from did:web:vc.lab.gaia-x.eu".config(enabled = false) {
        val resolvedDid = DidService.resolve("did:web:vc.lab.gaia-x.eu")
        val encoded = resolvedDid.encodePretty()
        println(encoded)
    }

    "get path from did:web" {
        val didUrl = DidUrl.from("did:web:wallet.waltid.org:api:did-registry:266fa44b20c247a9926b44f4263799a3")
        DidWeb.getPath(didUrl) shouldBe "api/did-registry/266fa44b20c247a9926b44f4263799a3"
    }

    "get path (empty) from did:web" {
        val didUrl = DidUrl.from("did:web:empty-path.com")
        DidWeb.getPath(didUrl) shouldBe ""
    }

    "get domain from did:web" {
        val didUrl = DidUrl.from("did:web:sub-domain.top-level-domain.com")
        DidWeb.getDomain(didUrl) shouldBe "sub-domain.top-level-domain.com"
    }

    "get domain from did:web incl. path" {
        val didUrl = DidUrl.from("did:web:sub-domain.top-level-domain.com:api:did-registry:266fa44b20c247a9926b44f4263799a3")
        DidWeb.getDomain(didUrl) shouldBe "sub-domain.top-level-domain.com"
    }

}) {

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)

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
    return did
}
