package id.walt.deprecated

import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import id.walt.model.DidMethod
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import id.walt.signatory.dataproviders.DefaultDataProvider
import id.walt.vclib.credentials.PermanentResidentCard
import id.walt.vclib.model.VerifiableCredential
import id.walt.vclib.templates.VcTemplateManager
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.apache.commons.codec.binary.Hex
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.net.URI
import java.security.Security

@Deprecated(message = "New version in package id.walt.service.vc")
class CredentialServiceTest : AnnotationSpec() {

    private val credentialService = JsonLdCredentialService.getService()
    private val keyService = KeyService.getService()

    private val RESOURCES_PATH: String = "src/test/resources"

    fun readCredOffer(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    fun readVerifiableCredential(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
        ServiceMatrix("$RESOURCES_PATH/service-matrix.properties")
    }


    //TOOD FIX @ signature-ld lib: the type in the proof is an ARRAY, rather than  a "type" : [ "Ed25519Signature2018" ],

    val testEd25519PrivateKeyString =
        "984b589e121040156838303f107e13150be4a80fc5088ccba0b0bdc9b1d89090de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580"

    val testEd25519PublicKeyString = "de8777a28f8da1a74e7a13090ed974d879bf692d001cddee16e4cc9f84b60580"


//    TODO
//    @Test
//    fun testSignEd25519Signature2018() {
//
//        val testEd25519PrivateKey = Hex.decodeHex(testEd25519PrivateKeyString.toCharArray())
//        val testEd25519PublicKey = Hex.decodeHex(testEd25519PublicKeyString.toCharArray())
//
//        val jsonLdObject = JsonLDObject.fromJson(File("src/test/resources/input.jsonld").readText())
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
//        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
//        val domain = "example.com"
//        val nonce: String? = null
//        val signer = Ed25519Signature2018LdSigner(testEd25519PrivateKey)
//        signer.creator = creator
//        signer.created = created
//        signer.domain = domain
//        signer.nonce = nonce
//        val ldProof: LdProof = signer.sign(jsonLdObject)
//        println(ldProof.toJson(true))
//        SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018.term shouldBe ldProof.type
//        creator shouldBe ldProof.creator
//        created shouldBe ldProof.created
//        domain shouldBe ldProof.domain
//        nonce shouldBe ldProof.nonce
//        "eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..5VI99nGh5wrAJRub5likTa5lLQ2Dmfiv-ByTRfd1D4WmnOSo3N1eSLemCYlXG95VY6Na-FuEHpjofI8iz8iPBQ" shouldBe ldProof.jws
//        val verifier = Ed25519Signature2018LdVerifier(testEd25519PublicKey)
//        val verify: Boolean = verifier.verify(jsonLdObject, ldProof)
//        verify shouldBe true
//    }

//    @Test
//    fun testSecp256k1Signature2018_LTSigner() {
//
//        val keyId = KeyManagementService.generateSecp256k1KeyPairSun()
//
//        val jsonLdObject = JsonLDObject.fromJson(File("src/test/resources/input.jsonld").readText())
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
//        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
//        val domain = "example.com"
//        val nonce: String? = null
//        //val signer = Ed25519Signature2018LdSigner(testEd25519PrivateKey)
//        val signer = EcdsaSecp256k1Signature2019LdSigner(KeyId(keyId))
//        signer.creator = creator
//        signer.created = created
//        signer.domain = domain
//        signer.nonce = nonce
//        val ldProof: LdProof = signer.sign(jsonLdObject)
//
//        println(ldProof.toJson(true))
//        SignatureSuites.SIGNATURE_SUITE_ECDSASECP256L1SIGNATURE2019.term shouldBe ldProof.type
//        creator shouldBe ldProof.creator
//        created shouldBe ldProof.created
//        domain shouldBe ldProof.domain
//        nonce shouldBe ldProof.nonce
//
//        val pubKey = KeyManagementService.loadKeys(keyId)!!.toEcKey().toECPublicKey()
//        val verifier = EcdsaSecp256k1Signature2019LdVerifier(pubKey)
//        val verify: Boolean = verifier.verify(jsonLdObject, ldProof)
//        verify shouldBe true
//    }

    @Test
    fun signEd25519Signature2018Test() {

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null
        val proof = ProofConfig(issuerDid = issuerDid, domain = domain, nonce = nonce)
        val cred = DefaultDataProvider.populate(VcTemplateManager.loadTemplate("VerifiableId"), proof).encode()

        val vc = credentialService.sign(cred, proof)
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc).verified
        vcVerified shouldBe true
        keyService.delete(issuerDid)
    }

    @Test
    fun signEcdsaSecp256k1Signature2019Test() {

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null
        val proof = ProofConfig(issuerDid = issuerDid, domain = domain, nonce = nonce)
        val cred = DefaultDataProvider.populate(VcTemplateManager.loadTemplate("VerifiableId"), proof).encode()

        val vc = credentialService.sign(cred, proof)
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc).verified
        vcVerified shouldBe true
        keyService.delete(issuerDid)
    }

    //@Test
    fun issueWorkHistoryCredential() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid, domain = domain, nonce = nonce))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc).verified
        vcVerified shouldBe true
    }

    //@Test
    fun issuePermanentResidentCardCredential() {

        val credOffer = readCredOffer("PermanentResidentCardExample")

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null

        val vc = credentialService.sign(credOffer, ProofConfig(issuerDid = issuerDid, domain = domain, nonce = nonce))
        vc shouldNotBe null
        println("Credential generated: $vc")

        val vcVerified = credentialService.verify(vc).verified
        vcVerified shouldBe true
        keyService.delete(issuerDid)
    }

    @Test
    fun issueVerifiablePresentation() {
        println("Generating PermanentResidentCard...")
        val data2: VerifiableCredential = PermanentResidentCard(
            credentialSubject = PermanentResidentCard.PermanentResidentCardSubject(
                id = "did:example:123",
                type = listOf(
                    "PermanentResident",
                    "Person"
                ),
                givenName = "JOHN",
                birthDate = "1958-08-17"
            ),
            issuer = "did:example:456",
            proof = id.walt.vclib.model.Proof(
                "Ed25519Signature2018",
                "2020-04-22T10:37:22Z",
                "assertionMethod",
                "did:example:456#key-1",
                "eyJjcml0IjpbImI2NCJdLCJiNjQiOmZhbHNlLCJhbGciOiJFZERTQSJ9..BhWew0x-txcroGjgdtK-yBCqoetg9DD9SgV4245TmXJi-PmqFzux6Cwaph0r-mbqzlE17yLebjfqbRT275U1AA"
            )
        )

        val vpInputStr = data2.encode()

        print(vpInputStr)

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null

        val vp = credentialService.sign(vpInputStr, ProofConfig(issuerDid = issuerDid, domain = domain, nonce = nonce))

        vp shouldNotBe null
        println("Verifiable Presentation generated: $vp")
    }

}
