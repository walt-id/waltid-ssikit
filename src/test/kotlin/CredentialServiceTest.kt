import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.provider.Ed25519Provider
import info.weboftrust.ldsignatures.crypto.provider.impl.TinkEd25519Provider
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.verifier.EcdsaSecp256k1Signature2019LdVerifier
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.URI
import java.security.Security
import java.util.*
import kotlin.test.*
import CredentialService.SignatureType.*

class CredentialServiceTest {

    protected val RESOURCES_PATH: String = "src/test/resources"

    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun signEd25519Signature2018Test() {

        val issuerDid = DidService.registerDid()
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(issuerDid, domain, nonce, cred, Ed25519Signature2018)
        assertNotNull(vc)
        println("Credential generated: ${vc}")

        var vcVerified = CredentialService.verify(issuerDid,vc, Ed25519Signature2018)
        assertTrue(vcVerified)
    }

    @Test
    fun signEcdsaSecp256k1Signature2019Test() {

        val keyId = KeyManagementService.generateSecp256k1KeyPair()
        val issuerDid = DidService.registerDid()
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val vc = CredentialService.sign(keyId, domain, nonce, cred, EcdsaSecp256k1Signature2019)
        assertNotNull(vc)
        println("Credential generated: ${vc}")

        var vcVerified = CredentialService.verify(keyId, vc, EcdsaSecp256k1Signature2019)
        assertTrue(vcVerified)
    }




    /******** following is test-code and should not be needed any more *******************/


    @Test
    fun issuerEcdsaSecp256k1CredentialTest() {

        val kms = KeyManagementService

        // val keyId = kms.generateEcKeyPair("P-256") // no way to populate ld-signatures with this key
        val keyId = kms.generateSecp256k1KeyPair()

        val issuerDid = DidService.registerDid(keyId)

        val cred: Map<String, String> = mapOf("one" to "two")


        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(JSONObject(cred).toString())
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER)

        val creator = URI.create(issuerDid)
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
        val domain = "example.com"
        val nonce: String? = null

        val issuerKeys = kms.loadKeys(issuerDid)

        val ecKey = ECKey.fromPrivate(issuerKeys!!.privateKey)

        val signer = EcdsaSecp256k1Signature2019LdSigner(ecKey)
        signer.creator = creator
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce
        val ldProof: LdProof = signer.sign(jsonLdObject)

        println("JsonLD proof: ${ldProof.toJson(true)}")

        val signedCredMap = HashMap<String, Any>(cred)
        signedCredMap.put("proof", JSONObject(ldProof.toJson()))
        val vc: String = JSONObject(signedCredMap).toString()

        println("Credential generated: ${vc}")

        val jsonLdObject2 = JsonLDObject.fromJson(vc)

        var ecKeyPub = ECKey.fromPublicOnly(issuerKeys!!.publicKey)
        val verifier = EcdsaSecp256k1Signature2019LdVerifier(ecKeyPub)
        val verified = verifier.verify(jsonLdObject)
        assert(verified)

    }

    @Test
    fun issuerWorkHistoryCredential() {

        val filePath = "$RESOURCES_PATH/credential-offers/WorkHistory.json"

        val credOffer = File(filePath).readText(Charsets.UTF_8)

        print(credOffer)
    }

    @Test
    fun dummyIssueEd25519CredentialTest() {

        val kms = KeyManagementService
        val ds = DidService
        val issuerDid = ds.registerDid()
        val holderDid = ds.registerDid()

        val cred: Map<String, String> = mapOf("one" to "two")

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(JSONObject(cred).toString())
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER)

        val creator = URI.create(issuerDid)
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
        val domain = "example.com"
        val nonce: String? = null

        val issuerKeys = kms.loadKeys(issuerDid)

        var signer = Ed25519Signature2018LdSigner(issuerKeys!!.getPrivateAndPublicKey())
        // following is working in version 0.4
        //var signer = Ed25519Signature2020LdSigner(issuerKeys!!.getPrivateAndPublicKey())

        signer.creator = creator
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce


        Ed25519Provider.set(TinkEd25519Provider())

        val ldProof: LdProof = signer.sign(jsonLdObject)

        println("JsonLD proof: ${ldProof.toJson(true)}")

        val signedCredMap = HashMap<String, Any>(cred)
        signedCredMap.put("proof", JSONObject(ldProof.toJson()))
        val vc: String = JSONObject(signedCredMap).toString()
        println(vc)
        val jsonLdObject2 = JsonLDObject.fromJson(vc)
        //TODO extract issuer
        val verifier = Ed25519Signature2018LdVerifier(issuerKeys!!.publicKey)
        var vcVerified = verifier.verify(jsonLdObject, ldProof)
        assertTrue(vcVerified)
    }

    private val testSecp256k1PrivateKeyString = "2ff4e6b73bc4c4c185c68b2c378f6b233978a88d3c8ed03df536f707f084e24e"
    private val testSecp256k1PublicKeyString = "0343f9455cd248e24c262b1341bbe37cea360e1c5ce526e5d1a71373ba6e557018"

    fun loadPrivateKey(issuer: String): ECKey =
        ECKey.fromPrivate(Hex.decodeHex(testSecp256k1PrivateKeyString.toCharArray()))

    fun loadPublicKey(issuer: String): ECKey =
        ECKey.fromPublicOnly(Hex.decodeHex(testSecp256k1PublicKeyString.toCharArray()))


    fun verifyDummy(vc: String): Boolean {
        val jsonLdObject = JsonLDObject.fromJson(vc)
        //TODO extract issuer
        val verifier = EcdsaSecp256k1Signature2019LdVerifier(loadPublicKey("dummyIssuer"))
        return verifier.verify(jsonLdObject)
    }

    @Test
    fun dummyIssueSecp256k1Issue() {
        val cred: Map<String, String> = mapOf("one" to "two")

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(JSONObject(cred).toString())
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER)

        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
        val domain = "example.com"
        val nonce: String? = null

        val signer = EcdsaSecp256k1Signature2019LdSigner(loadPrivateKey("dummyIssuer"))
        signer.creator = creator
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce
        val ldProof: LdProof = signer.sign(jsonLdObject)

        println("JsonLD proof: ${ldProof.toJson(true)}")

        val signedCredMap = HashMap<String, Any>(cred)
        signedCredMap.put("proof", JSONObject(ldProof.toJson()))
        val vc: String = JSONObject(signedCredMap).toString()

        print("Credential generated: ${vc}")

        verifyDummy(vc)

    }
}
