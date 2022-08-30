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
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
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
    fun createDidEbsiV1Identifier() {
        val didUrl = DidUrl.generateDidEbsiV1DidUrl()
        val did = didUrl.did
        did.substring(0, 9) shouldBe  "did:ebsi:"
        didUrl.identifier.length shouldBeOneOf listOf(23, 24)
        didUrl.identifier[0] shouldBe 'z'
        didUrl.identifier.substring(1).decodeBase58()[0] shouldBe 0x01
        didUrl.method shouldBe "ebsi"
    }

    @Test
    fun createDidEbsiV2Identifier() {
        val keyService = KeyService.getService()
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val publicKeyJwk = keyService.toJwk(keyId.id, KeyType.PUBLIC)
        val publicKeyJwkThumbprint = publicKeyJwk.computeThumbprint().decode()
        val didUrl = DidUrl.generateDidEbsiV2DidUrl(publicKeyJwkThumbprint)
        val did = didUrl.did
        did.substring(0, 9) shouldBe  "did:ebsi:"
        didUrl.identifier.length shouldBe 45
        didUrl.identifier[0] shouldBe 'z'
        didUrl.identifier.substring(1).decodeBase58()[0] shouldBe 0x02
        didUrl.method shouldBe "ebsi"
    }

    @Test
    fun createDidEbsiTest() {

        // Create
        val keyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val did = ds.create(DidMethod.ebsi, keyId.id)

        // Load
        val resolvedDid = ds.load(did) as DidEbsi
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        // Update
        resolvedDid.assertionMethod = listOf(resolvedDid.verificationMethod!!.get(0).toReference())
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

    @Test
    fun testDeleteDid() {
        forAll(
            row(DidMethod.key, null),
            row(DidMethod.web, null),
            row(DidMethod.ebsi, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.RSA).id),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.RSA).id),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.RSA).id),
        ) { method, kid ->
            val did = ds.create(method, kid)
            val ids = ds.load(did).verificationMethod?.map { it.id }
            ds.deleteDid(did)
            shouldThrow<Exception> { ds.load(did) }
            ids?.forEach {
                println(it)
                shouldThrow<Exception> {
                    keyService.load(it)
                }
            }
        }
    }

    @Test
    fun testParseAndSerializeDidWithMixedVerificationRelationships() {
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
            "  \"capabilityInvocation\": [\n" +
            "    \"did:ebsi:zcGvqgZTHCtkjgtcKRL7H8k#keys-1\",\n" +
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
            "  ]\n" +
            "}"

        val did = Did.decode(didDoc)
        did!!.capabilityInvocation!!.size shouldBe 2
        did.capabilityInvocation!![0].isReference shouldBe true
        did.capabilityInvocation!![1].isReference shouldBe false

        val reEncodedDid = did.encode()
        reEncodedDid shouldMatchJson didDoc
    }
}
