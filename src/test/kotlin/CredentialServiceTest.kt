import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.provider.Ed25519Provider
import info.weboftrust.ldsignatures.crypto.provider.impl.TinkEd25519Provider
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.suites.SignatureSuites
import info.weboftrust.ldsignatures.verifier.EcdsaSecp256k1Signature2019LdVerifier
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import junit.framework.Assert.assertTrue
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader
import java.net.URI
import java.security.Security
import java.util.HashMap
import kotlin.test.assertNotNull

class CredentialServiceTest {


    @Before
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
    }

    fun verifyEcdsaSecp256k1Credential(vc: String): Boolean {
        val jsonLdObject = JsonLDObject.fromJson(vc)
        // TODO Load from KMS
        val verifier = EcdsaSecp256k1Signature2019LdVerifier(loadPublicKey("dummyIssuer"))
        return verifier.verify(jsonLdObject)
    }

    @Test
    fun issueEd25519CredentialTest() {
        val kms = KeyManagementService
        val ds = DidService
        val cs = CredentialService

        val issuerDid = ds.registerDid()
        val domain = "example.com"
        val nonce: String? = null
        val credMap: Map<String, String> = mapOf("one" to "two")
        val cred = JSONObject(credMap).toString()

        val proof = cs.signEd25519Signature2018(issuerDid, domain, nonce, cred)
        assertNotNull(proof)

        val vc = cs.addProof(credMap, proof)
        assertNotNull(vc)

        println("Credential generated: ${vc}")

        val ret = cs.verifyEd25519Signature2018(issuerDid, vc)
        assertTrue(ret)
    }


    // TODO: Fix key handling -> need to generate key using
    /// @Test
    fun issuerEcdsaSecp256k1CredentialTest() {

        val kms = KeyManagementService
        val ds = DidService

        val keyId = kms.generateEcKeyPair("P-256")

        val issuerDid = ds.registerDid(keyId)
        val holderDid = ds.registerDid()

        val cred: Map<String, String> = mapOf("one" to "two")


        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(JSONObject(cred).toString())
        jsonLdObject.setDocumentLoader(LDSecurityContexts.DOCUMENT_LOADER)

        val creator = URI.create(issuerDid)
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
        val domain = "example.com"
        val nonce: String? = null

        val issuerKeys = kms.loadKeys(kms.getKeyId(issuerDid)!!)
        val privateKeyBytes = issuerKeys!!.pair!!.private.encoded

        val key: BCECPrivateKey = issuerKeys!!.pair!!.private as BCECPrivateKey

        println(key.parameters.curve)

        // FIX: following is not working, as the key is in encoded form. we would need the raw bytes
        val ecKey = ECKey.fromASN1(null)


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

        val ret = verifyEcdsaSecp256k1Credential(vc)

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

        val issuerKeys = kms.loadKeys(kms.getKeyId(issuerDid)!!)

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

//    @Test
//    @Throws(Throwable::class)
//    fun testSignEd25519Signature2018() {
//        val jsonLdObject = JsonLDObject.fromJson(
//            InputStreamReader(
//                info.weboftrust.ldsignatures.JsonLdSignEd25519Signature2018Test::class.java.getResourceAsStream("input.jsonld")
//            )
//        )
//        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER
//        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
//        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")
//        val domain = "example.com"
//        val nonce: String? = null
//        val signer = Ed25519Signature2018LdSigner(TestUtil.testEd25519PrivateKey)
//        signer.creator = creator
//        signer.created = created
//        signer.domain = domain
//        signer.nonce = nonce
//        val ldProof: LdProof = signer.sign(jsonLdObject)
//        assertEquals(SignatureSuites.SIGNATURE_SUITE_ED25519SIGNATURE2018.term, ldProof.type)
//        assertEquals(creator, ldProof.creator)
//        assertEquals(created, ldProof.created)
//        assertEquals(domain, ldProof.domain)
//        assertEquals(nonce, ldProof.nonce)
//        // assertEquals("eyJiNjQiOmZhbHNlLCJjcml0IjpbImI2NCJdLCJhbGciOiJFZERTQSJ9..5VI99nGh5wrAJRub5likTa5lLQ2Dmfiv-ByTRfd1D4WmnOSo3N1eSLemCYlXG95VY6Na-FuEHpjofI8iz8iPBQ", ldProof.getJws());
//        val verifier = Ed25519Signature2018LdVerifier(TestUtil.testEd25519PublicKey)
//        val verify: Boolean = verifier.verify(jsonLdObject, ldProof)
//        assertTrue(verify)
//    }


    /******** following should not be needed any more *******************/

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
