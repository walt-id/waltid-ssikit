import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.EcdsaSecp256k1Signature2019LdSigner
import info.weboftrust.ldsignatures.verifier.EcdsaSecp256k1Signature2019LdVerifier
import org.apache.commons.codec.binary.Hex
import org.bitcoinj.core.ECKey
import java.io.StringReader
import java.net.URI
import javax.json.Json
import javax.json.JsonObject
import javax.json.JsonReader

class IssuerService {

    private val testSecp256k1PrivateKeyString = "2ff4e6b73bc4c4c185c68b2c378f6b233978a88d3c8ed03df536f707f084e24e"
    private val testSecp256k1PublicKeyString =  "0343f9455cd248e24c262b1341bbe37cea360e1c5ce526e5d1a71373ba6e557018"

    // TODO: Load key from KMS
    fun loadPrivateKey(issuer: String): ECKey =
        ECKey.fromPrivate(Hex.decodeHex(testSecp256k1PrivateKeyString.toCharArray()))

    fun loadPublicKey(issuer: String): ECKey =
        ECKey.fromPublicOnly(Hex.decodeHex(testSecp256k1PublicKeyString.toCharArray()))

    private fun sign(creator: URI, domain: String, nonce: String?, jsonCred: String): JsonObject {

        // log.info("Signing: $jsonCred")

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = false
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")

        val signer = EcdsaSecp256k1Signature2019LdSigner(loadPrivateKey("dummyIssuer"))
        signer.creator = creator
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce
        val ldProof: LdProof = signer.sign(jsonLdObject)
        return ldProof.toJsonObject()
    }


    fun issue(credential: String): String {

        //TODO extract the following data out of the credential
        val creator = URI.create("did:sov:WRfXPg8dantKVubE3HX8pw")
        val domain = "example.com"
        val nonce: String? = null

        val signatureObject = sign(creator, domain, nonce, credential)
        //println("Proof $signatureObject")

        //val reader: JsonReader = Json.createReader(FileReader("jsondata.txt"))
        val reader: JsonReader = Json.createReader(StringReader(credential))
        val cred = reader.readObject()
        reader.close()

        val vc = Json.createObjectBuilder(cred).add("proof", signatureObject).build()

        return vc.toString()
    }

    fun verify(vc: String): Boolean {
        val jsonLdObject = JsonLDObject.fromJson(vc)
        //TODO extract issuer
        val verifier = EcdsaSecp256k1Signature2019LdVerifier(loadPublicKey("dummyIssuer"))
        return verifier.verify(jsonLdObject)
    }

    fun issue() {
        print("issue")
    }
}
