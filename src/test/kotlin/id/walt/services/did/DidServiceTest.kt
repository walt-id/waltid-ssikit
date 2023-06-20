package id.walt.services.did

import com.beust.klaxon.Klaxon
import id.walt.common.prettyPrint
import id.walt.common.readWhenContent
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.decodeBase58
import id.walt.model.Did
import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.model.did.DidEbsi
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.test.RESOURCES_PATH
import io.kotest.assertions.json.shouldMatchJson
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldBeOneOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import java.io.File

class DidServiceTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val keyService = KeyService.getService()
    private val webOptions = DidWebCreateOptions("walt.id")

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    val ds = DidService

    private fun assertVerificationMethodAliases(did: Did) {
        did.verificationMethod shouldNotBe null
        did.verificationMethod!!.forEach { vm ->
            keyService.hasKey(vm.id) shouldBe true
        }
    }

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

        assertVerificationMethodAliases(resolvedDid)
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

        assertVerificationMethodAliases(resolvedDid)
    }

    @Test
    fun createResolveDidKeySecpTest() {
        val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256k1)

        // Create
        val did = ds.create(DidMethod.key, keyId.id)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        didUrl.identifier shouldStartWith "zQ3s"
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        assertVerificationMethodAliases(resolvedDid)

        val originalKeyJwk =
            KeyService.getService().toJwk(keyId.id, jwkKeyId = resolvedDid.verificationMethod!![0].publicKeyJwk!!.kid)
        Klaxon().toJsonString(resolvedDid.verificationMethod!![0].publicKeyJwk) shouldMatchJson originalKeyJwk.toJSONString()
    }

    @Test
    fun createResolveDidKeyP256Test() {
        val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256r1)

        // Create
        val did = ds.create(DidMethod.key, keyId.id)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "key" shouldBe didUrl.method
        didUrl.identifier shouldStartWith "zDn"
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        assertVerificationMethodAliases(resolvedDid)

        val originalKeyJwk =
            KeyService.getService().toJwk(keyId.id, jwkKeyId = resolvedDid.verificationMethod!![0].publicKeyJwk!!.kid)
        Klaxon().toJsonString(resolvedDid.verificationMethod!![0].publicKeyJwk) shouldMatchJson originalKeyJwk.toJSONString()
    }

    @Test
    fun createResolveDidJwkP256Test() {
        val keyId = KeyService.getService().generate(KeyAlgorithm.ECDSA_Secp256r1)

        // Create
        val did = ds.create(DidMethod.jwk, keyId.id)
        val didUrl = DidUrl.from(did)
        did shouldBe didUrl.did
        "jwk" shouldBe didUrl.method
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Klaxon().toJsonString(resolvedDid)
        println(encoded)

        assertVerificationMethodAliases(resolvedDid)
    }

    @Test
    fun testResolveDidJwkExamples() {
        val didDoc =
            DidService.resolve("did:jwk:eyJjcnYiOiJQLTI1NiIsImt0eSI6IkVDIiwieCI6ImFjYklRaXVNczNpOF91c3pFakoydHBUdFJNNEVVM3l6OTFQSDZDZEgyVjAiLCJ5IjoiX0tjeUxqOXZXTXB0bm1LdG00NkdxRHo4d2Y3NEk1TEtncmwyR3pIM25TRSJ9")
        didDoc.method shouldBe DidMethod.jwk
        didDoc.verificationMethod!!.first().publicKeyJwk!!.crv shouldBe "P-256"
        didDoc.verificationMethod!!.first().publicKeyJwk!!.kty shouldBe "EC"
        didDoc.verificationMethod!!.first().publicKeyJwk!!.x shouldBe "acbIQiuMs3i8_uszEjJ2tpTtRM4EU3yz91PH6CdH2V0"
        didDoc.verificationMethod!!.first().publicKeyJwk!!.y shouldBe "_KcyLj9vWMptnmKtm46GqDz8wf74I5LKgrl2GzH3nSE"
    }

    @Test
    fun createDidEbsiV1Identifier() {
        val didUrl = DidUrl.generateDidEbsiV1DidUrl()
        val did = didUrl.did
        did.substring(0, 9) shouldBe "did:ebsi:"
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
        did.substring(0, 9) shouldBe "did:ebsi:"
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

        assertVerificationMethodAliases(resolvedDid)

        // Update
        resolvedDid.assertionMethod = listOf(resolvedDid.verificationMethod!![0].toReference())
        ds.updateDidEbsi(resolvedDid)
        val encodedUpd = Klaxon().toJsonString(resolvedDid)
        println(encodedUpd)
    }

    @Test
    @Ignore // TODO: ESSIF backend issue
    fun resolveDidEbsiTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolve(did)
        val encDidEbsi = Klaxon().toJsonString(didDoc)
        println(encDidEbsi)
    }

    @Test
    @Ignore//Fix http.400 - did must be a valid DID v1
    fun resolveDidEbsiRawTest() {
        val did = "did:ebsi:22S7TBCJxzPS2Vv1UniBSdzFD2ZDFjZeYvQuFQWSeAQN5nTG"
        val didDoc = DidService.resolve(did, DidEbsiResolveOptions(true))
        println(didDoc.prettyPrint())
    }

    @Test
    fun resolveDidKeyJwkJcsPub(){
        // given
        val expectedResult = Did.decode(readWhenContent(File("src/test/resources/dids/did-key-jwk_jcs-pub.json")))!!
        val jwkPubKey = "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"ngy44T1vxAT6Di4nr-UaM9K3Tlnz9pkoksDokKFkmNc\",\"y\":\"QCRfOKlSM31GTkb4JHx3nXB4G_jSPMsbdjzlkT_UpPc\"}"
        val keyId = keyService.importKey(jwkPubKey)
        val did = DidService.create(DidMethod.key, keyId.id, DidKeyCreateOptions(useJwkJcsPub = true))
        // when
        val result = DidService.resolve(did)
        // then
        result.encodePretty() shouldBe expectedResult.encodePretty()
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
        val didDoc =
            "{\"authentication\" : [\"did:ebsi:zuffrvD4gvopW2dgTWDYTXv#87d651a26f3a416bba58770de899e8fe\"], \"@context\" : [\"https://www.w3.org/ns/did/v1\", \"https://www.w3.org/ns/did/v2\"], \"id\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv\", \"verificationMethod\" : [{\"controller\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv\", \"id\" : \"did:ebsi:zuffrvD4gvopW2dgTWDYTXv#87d651a26f3a416bba58770de899e8fe\", \"publicKeyJwk\" : {\"alg\" : \"EdDSA\", \"crv\" : \"Ed25519\", \"kid\" : \"87d651a26f3a416bba58770de899e8fe\", \"kty\" : \"OKP\", \"use\" : \"sig\", \"x\" : \"tP7zl2umgGKVMao41TkvjHBgu6EPebcnTmF9MuJqzlc\"}, \"type\" : \"Ed25519VerificationKey2018\"}]}"
        val did = Did.decode(didDoc)
        did shouldNotBe null
        did?.javaClass shouldBe DidEbsi::class.java
        println(did?.encodePretty())
        did?.encodePretty() shouldMatchJson didDoc
    }

    @Test
    fun testDeleteDid() {
        forAll(
            row(DidMethod.key, null, null),
            row(DidMethod.web, null, webOptions),
            row(DidMethod.ebsi, null, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.key, keyService.generate(KeyAlgorithm.RSA).id, null),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, webOptions),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, webOptions),
            row(DidMethod.web, keyService.generate(KeyAlgorithm.RSA).id, webOptions),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id, null),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.EdDSA_Ed25519).id, null),
            row(DidMethod.ebsi, keyService.generate(KeyAlgorithm.RSA).id, null),
        ) { method, kid, options ->
            val did = ds.create(method, kid, options)
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

    @Test
    fun testDidImportFromMauro() {
        val did =
            "did:jwk:eyJrdHkiOiJFQyIsIngiOiJuSkpwZzgzY084R0sxZmZ6dmtoaXFabjk4eEczS3ctbEJGX1llZ1hpVzNzIiwieSI6ImhtUU1OVU5Ta3lycGlydmoxanhzbTZBbENCd2lmdWdhQ1NYLWpVaDNXZkkiLCJjcnYiOiJQLTI1NiJ9"
        shouldNotThrowAny {
            DidService.importDidAndKeys(did)
        }
    }

    @Test
    fun testDidImportFromBram() {
        val did =
            "did:jwk:eyJ1c2UiOiJzaWciLCJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6Ik1WeUxseS1WcDNZbF9DQlVBQTdQR1dZemZMSVhsYzlINzNfMmItWXJUeEEiLCJ5IjoiM3UwR2JFaWw1RU41NnJta1d3R2tmVENOdERXZ3JCb1BhV2FVVFRZTU9yayJ9"
        shouldNotThrowAny {
            DidService.importDidAndKeys(did)
        }
    }

    @Test
    fun testDidWithoutContext() {
        val did = "{\n" +
                "  \"id\": \"did:peer:0z6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv\",\n" +
                "  \"authentication\": [\n" +
                "    {\n" +
                "      \"id\": \"did:peer:0z6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv#6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv\",\n" +
                "      \"type\": \"Ed25519VerificationKey2020\",\n" +
                "      \"controller\": \"did:peer:0z6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv\",\n" +
                "      \"publicKeyMultibase\": \"z6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        val parsedDid = Did.decode(did)
        parsedDid?.id shouldNotBe null
        parsedDid!!.id shouldBe "did:peer:0z6Mksu6Kco9yky1pUAWnWyer17bnokrLL3bYvYFp27zv8WNv"
        parsedDid.context shouldBe null

        parsedDid.encode() shouldMatchJson did
    }
}
