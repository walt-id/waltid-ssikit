package id.walt.services.vc

import id.walt.credentials.w3c.*
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.schema.SchemaValidatorFactory
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.test.RESOURCES_PATH
import id.walt.test.getTemplate
import id.walt.test.readCredOffer
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockkObject
import java.io.File
import java.net.URI

class WaltIdJsonLdCredentialServiceTest : AnnotationSpec() {

    init {
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }

    private val credentialService = JsonLdCredentialService.getService()
    private val issuerWebDid = DidService.create(DidMethod.web, options = DidWebCreateOptions("walt.id"))
    private val issuerKeyDid = DidService.create(DidMethod.key)
    private val subjectKeyDid = DidService.create(DidMethod.key)
    private val anotherKeyDid = DidService.create(DidMethod.key)
    private val issuerEbsiDid = DidService.create(DidMethod.ebsi)

    val VC_PATH = "src/test/resources/verifiable-credentials"

    @BeforeAll
    fun setup() {
      mockkObject(SchemaValidatorFactory)
      every { SchemaValidatorFactory.get(URI.create("https://api-pilot.ebsi.eu/trusted-schemas-registry/v1/schemas/0xb77f8516a965631b4f197ad54c65a9e2f9936ebfb76bae4906d33744dbcc60ba"))}.returns(
        SchemaValidatorFactory.get(URI.create("https://raw.githubusercontent.com/walt-id/waltid-ssikit-vclib/master/src/test/resources/schemas/VerifiableId.json").toURL().readText())
      )
    }

    fun genericSignVerify(issuerDid: String, credOffer: String) {

        val vcStr = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid))
        println("Credential generated: $vcStr")

        val vc = vcStr.toVerifiableCredential()
        println("Credential decoded: $vc")

        println("Verifying...")

        val vcVerified = credentialService.verify(vcStr)

        vcVerified.verified shouldBe true

        vcVerified.verificationType shouldBe VerificationType.VERIFIABLE_CREDENTIAL

        val holderDid = vc.subjectId!!
        val vpStr = credentialService.present(listOf(vcStr.toPresentableCredential()), holderDid, "domain.com", "nonce", null)
        println("Presentation generated: $vpStr")

        val vp = vpStr.toVerifiablePresentation()
        println(vpStr)
        vp.proof?.domain shouldBe "domain.com"
        vp.proof?.nonce shouldBe "nonce"
        vp.proof?.proofPurpose shouldBe "authentication"
        vp.proof?.verificationMethod shouldBe DidService.load(holderDid).authentication?.get(0)?.id

        val vpVerified = credentialService.verify(vpStr)
        vpVerified.verified shouldBe true
        VerificationType.VERIFIABLE_PRESENTATION shouldBe vpVerified.verificationType
    }

    @Test
    fun signEbsiVerifiableAttestation() {
        val template = getTemplate("ebsi-attestation")

        template.issuer = W3CIssuer(issuerWebDid)
        template.credentialSubject!!.id = issuerWebDid // self signed

        val credOffer = template.toJson()

        genericSignVerify(issuerWebDid, credOffer)
    }

    @Test
    fun signEuropass() {
        val template = getTemplate("europass")
        val builder = W3CCredentialBuilder.fromPartial(template)
            .setIssuerId(issuerKeyDid)
            .buildSubject {
                setId(issuerKeyDid) // self signed
                setProperty("achieved", listOf(
                    buildMap {
                        put("title", "Some Europass specific title")
                    }
                ))
            }

        val credOffer = builder.build().toJson()

        println("GENERIC SIGN VERIFY")
        genericSignVerify(issuerKeyDid, credOffer)
    }

    @Test
    fun signPermanentResidentCard() {
        val builder = W3CCredentialBuilder.fromPartial(getTemplate("permanent-resident-card"))
            .setIssuerId(issuerKeyDid)
            .buildSubject {
                setId(issuerKeyDid)
                setProperty("givenName", "Given Name")
            }

        val credOffer = builder.build().toJson()

        genericSignVerify(issuerKeyDid, credOffer)
    }

    // TODO: create DID for holder @Test
    fun presentVa() {
        val vaStr = File("$VC_PATH/vc-ebsi-verifiable-authorisation.json").readText()

        val vp = credentialService.present(listOf(vaStr.toPresentableCredential()), vaStr.toVerifiableCredential().subjectId!!, null, null, null)

        println(vp)
    }

    @Test
    fun presentEuropassTest() {

        val domain = "example.com"
        val challenge = "asdf"

        val builder = W3CCredentialBuilder.fromPartial(getTemplate("europass"))
            .setIssuerId(issuerEbsiDid)
            .buildSubject { setId(subjectKeyDid) }

        val vc = credentialService.sign(builder.build().encode(), ProofConfig(issuerDid = issuerEbsiDid))

        println("Signed vc: $vc")
        val vcSigned = vc.toVerifiableCredential()
        println(vcSigned.toString())

        val vp = credentialService.present(listOf(vc.toPresentableCredential()), vcSigned.subjectId!!, domain, challenge, null)
        println("Presentation generated: $vp")

        val vpVerified = credentialService.verify(vp)
        vpVerified.verified shouldBe true

    }


    // TODO: consider methods below, as old data-model might be used
    @Test
    fun signCredentialECDSASecp256k1Test() {
        val domain = "example.com"
        val nonce: String? = null
        val proof = ProofConfig(subjectDid = issuerWebDid, issuerDid = issuerWebDid, nonce = nonce, domain = domain)
        val credOffer = W3CCredentialBuilder.fromPartial(readCredOffer("WorkHistory"))
            .setIssuerId(issuerWebDid)
            .build().encode()

        val vc = credentialService.sign(credOffer, proof)
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc)
        vcVerified.verified shouldBe true
    }

    @Test
    fun signCredentialEd25519k1Test() {
        val domain = "example.com"
        val nonce: String? = null

        val proof = ProofConfig(subjectDid = issuerKeyDid, issuerDid = issuerKeyDid, nonce = nonce, domain = domain)
        val credOffer = W3CCredentialBuilder.fromPartial(readCredOffer("WorkHistory"))
            .setIssuerId(issuerKeyDid).build().encode()

        val vc = credentialService.sign(credOffer, proof)
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc)
        vcVerified.verified shouldBe true
    }

    @Test
    fun signEuropassCredentialTest() {

        val domain = "example.com"
        val nonce: String? = null
        val proof = ProofConfig(subjectDid = issuerKeyDid, issuerDid = issuerKeyDid, nonce = nonce, domain = domain)
        val credOffer =
            W3CCredentialBuilder.fromPartial(readCredOffer("VerifiableAttestation-Europass"))
                .setIssuerId(issuerKeyDid).build().encode()

        val vc = credentialService.sign(credOffer, proof)
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc)
        vcVerified.verified shouldBe true
    }

    @Test
    fun testValidateSchemaTsr() {
        val issuerKeyDidDoc = DidService.load(issuerKeyDid)
        val issuerKeyDidVM = issuerKeyDidDoc.assertionMethod!!.first().id
        val noSchemaVc = VerifiableCredential().encode()
        val validVc = Signatory.getService().issue(
            "VerifiableId", ProofConfig(
                issuerDid = issuerKeyDid,
                subjectDid = subjectKeyDid,
                proofPurpose = "assertionMethod",
                issuerVerificationMethod = issuerKeyDidVM,
                proofType = ProofType.LD_PROOF
            )
        )
        val invalidDataVc = Signatory.getService().issue(
            W3CCredentialBuilder().setCredentialSchema(validVc.toVerifiableCredential().credentialSchema!!)
                .buildSubject { setProperty("foo", "bar") }, ProofConfig(
                issuerDid = issuerKeyDid,
                proofType = ProofType.LD_PROOF
            )
        )
        val notParsableVc = ""

        credentialService.validateSchemaTsr(noSchemaVc).isSuccess shouldBe false
        credentialService.validateSchemaTsr(validVc).isSuccess shouldBe true
        credentialService.validateSchemaTsr(invalidDataVc).isSuccess shouldBe false
        credentialService.validateSchemaTsr(notParsableVc).isSuccess shouldBe false
    }

    /*@Test
    fun signCredentialInvalidDataTest() {

        val credOffer = readCredOffer("vc-offer-simple-example")
        val issuerDid = DidService.create(DidMethod.key)

        val vcStr = credentialService.sign(issuerDid, credOffer)
        println("Credential generated: $vcStr")
        val vcInvalid = VerifiableCredential.fromString(vcStr)
        vcInvalid.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encode()
        println("Credential generated: ${vcInvalidStr}")

        val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)
        vcVerified shouldBe false
    }*/

// TODO @Test
    /*fun presentValidCredentialTest() {

        val credOffer = Klaxon().parse<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString()
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        val vcReqEnc = Klaxon().toJsonString(credOffer)

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        val vc = Klaxon().parse<VerifiableCredential>(vcStr)
        println("Credential generated: ${vc.encodePretty()}")

        val vpIn = VerifiablePresentation(
            listOf("https://www.w3.org/2018/credentials/v1"),
            "id",
            listOf("VerifiablePresentation"),
            listOf(vc),
            null
        )
        val vpInputStr = Klaxon().toJsonString(vpIn)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
        vp shouldNotBe null
        println("Verifiable Presentation generated: $vp")

        var ret = credentialService.verifyVp(vp)
        assertTrue { ret }
    }

    //TODO @Test
    fun presentInvalidCredentialTest() {
        val issuerDid = DidService.create(DidMethod.web)
        val subjectDid = DidService.create(DidMethod.key)

        val credOffer = Klaxon().parse<VerifiableCredential>(readCredOffer("vc-offer-simple-example"))
        //      credOffer.id = Timestamp.valueOf(LocalDateTime.now()).time.toString() // This line is causing LD-Signatures to fail (will produces a valid signature, although the data is invalidated below)
        credOffer.issuer = issuerDid
        credOffer.credentialSubject.id = subjectDid

        credOffer.issuanceDate = LocalDateTime.now()

        //val vcReqEnc = readCredOffer("vc-offer-simple-example") -> produces false-signature for invalid credential
        val vcReqEnc = Json {
            prettyPrint = true
        }.encodeToString(credOffer) // FIXME does not produce false-signature for invalid credential

        println("Credential request:\n$vcReqEnc")

        val vcStr = credentialService.sign(issuerDid, vcReqEnc)
        println("Credential generated: $vcStr")
        val vcInvalid = Klaxon().parse<VerifiableCredential>(vcStr)
        vcInvalid.credentialSubject.id = "INVALID ID"
        val vcInvalidStr = vcInvalid.encodePretty()
        println("Credential generated: ${vcInvalidStr}")

        val vcValid = credentialService.verifyVc(issuerDid, vcInvalidStr)
        vcValid shouldBe false

        val vcVerified = credentialService.verifyVc(issuerDid, vcInvalidStr)

        val vpIn = VerifiablePresentation(
            listOf("https://www.w3.org/2018/credentials/v1"),
            "id",
            listOf("VerifiablePresentation"),
            listOf(vcInvalid),
            null
        )
        val vpInputStr = Klaxon().toJsonString(vpIn)

        print(vpInputStr)

        val domain = "example.com"
        val nonce: String? = "asdf"
        val vp = credentialService.sign(issuerDid, vpInputStr, domain, nonce)
        vp shouldNotBe null
        println("Verifiable Presentation generated: $vp")

        var ret = credentialService.verifyVp(vp)
        ret shouldBe false
    }*/
}
