package id.walt.services.did

import com.beust.klaxon.Klaxon
import id.walt.common.prettyPrint
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.decodeBase58
import id.walt.model.Did
import id.walt.model.DidEbsi
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

class DidServiceTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val keyService = KeyService.getService()

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    val ds = DidService

    @Test
    fun parseDidUrlTest() {

        val didUrl = DidUrl("method", "identifier", "key1")
        "did:method:identifier#key1" shouldBe didUrl.url

        val obj: DidUrl = DidUrl.from(didUrl.url)
        didUrl shouldBe obj
    }

    @Test
    fun createResolveDidKeyTest() {

        // Create
        val did = ds.create(DidMethod.key)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createResolveDidKeyRsaTest() {
        val keyId = KeyService.getService().generate(KeyAlgorithm.RSA)

        // Create
        val did = ds.create(DidMethod.key, keyId.id)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createResolveDidKeySecpTest() {
        val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

        // Create
        val did = ds.create(DidMethod.key, keyId.id)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createDidEbsiV2Identifier() {
        val didUrl = DidUrl.generateDidEbsiV2DidUrl()
        val did = didUrl.did
        did.substring(0, 9) shouldBe  "did:ebsi:"
        didUrl.identifier.length shouldBeOneOf listOf(23, 24)
        didUrl.identifier[0] shouldBe 'z'
        didUrl.identifier.substring(1).decodeBase58()[0] shouldBe 0x01
        didUrl.method shouldBe "ebsi"
    }

    @Test
    fun createDidEbsiTest() {

        // Create
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val did = ds.create(DidMethod.ebsi, keyId.id)

        // Load
        val resolvedDid = ds.loadDidEbsi(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        // Update
        resolvedDid.assertionMethod = listOf(resolvedDid.verificationMethod!!.get(0).id)
        ds.updateDidEbsi(resolvedDid)
        val encodedUpd = Klaxon().toJsonString(resolvedDid)
        println(encodedUpd)
    }

    @Test
    @Ignore // TODO: ESSIF backend issue
    fun resolveDidEbsiTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolveDidEbsi(did)
        val encDidEbsi = Klaxon().toJsonString(didDoc)
        println(encDidEbsi)
    }

    @Test
    @Ignore // TODO: ESSIF backend issue
    fun resolveDidEbsiRawTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolveDidEbsiRaw(did)
        println(didDoc.prettyPrint())
    }

    @Test
    fun listDidsTest() {

        ds.create(DidMethod.key)

        val dids = ds.listDids()

        dids.isNotEmpty() shouldBe true

        dids.forEach { s -> s shouldBe DidUrl.from(s).did }
    }

    @Test
    fun parseDidWithSingleValueContext() {
        val didDoc = "{\n" +
            "  \"@context\": \"https://www.w3.org/ns/did/v1\",\n" +
            "  \"id\": \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k\",\n" +
            "  \"verificationMethod\": [\n" +
            "    {\n" +
            "      \"id\": \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k#keys-1\",\n" +
            "      \"type\": \"Secp256k1VerificationKey2018\",\n" +
            "      \"controller\": \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k\",\n" +
            "      \"publicKeyJwk\": {\n" +
            "        \"kty\": \"EC\",\n" +
            "        \"crv\": \"secp256k1\",\n" +
            "        \"x\": \"Iq579rsuHREntinz8NnlG_e8gDjNNQt4DbChj9mBt7Y\",\n" +
            "        \"y\": \"V-Tr9B56eA7H_UJN9q6dyMWlYkQkHFvtvDDlE66LXkk\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"authentication\": [\n" +
            "    \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k#keys-1\"\n" +
            "  ],\n" +
            "  \"assertionMethod\": [\n" +
            "    \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k#keys-1\"\n" +
            "  ]\n" +
            "}"
        val did = Did.decode(didDoc)
        did shouldNotBe null
        did?.javaClass shouldBe DidEbsi::class.java
        println(did?.encodePretty())
        did?.encodePretty() shouldMatchJson didDoc
    }

    @Test
    fun parseDidWithContextArray() {
        val didDoc = "{\"authentication\" : [\"did:ebsi:zuffrvD4gvopW2dgTWDYTXv#87d651a26f3a416bba58770de899e8fe\"], \"@context\" : [\"https://www.w3.org/ns/did/v1\", \"https://www.w3.org/ns/did/v2\"], \"id\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv\", \"verificationMethod\" : [{\"controller\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv\", \"id\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv#87d651a26f3a416bba58770de899e8fe\", \"publicKeyJwk\" : {\"alg\" : \"EdDSA\", \"crv\" : \"Ed25519\", \"kid\" : \"87d651a26f3a416bba58770de899e8fe\", \"kty\" : \"OKP\", \"use\" : \"sig\", \"x\" : \"tP7zl2umgGKVMao41TkvjHBgu6EPebcnTmF9MuJqzlc\"}, \"type\" : \"Ed25519VerificationKey2018\"}]}"
        val did = Did.decode(didDoc)
        did shouldNotBe null
        did?.javaClass shouldBe DidEbsi::class.java
        println(did?.encodePretty())
        did?.encodePretty() shouldMatchJson didDoc
    }
}
