import foundation.identity.jsonld.ConfigurableDocumentLoader
import foundation.identity.jsonld.JsonLDObject
import foundation.identity.jsonld.JsonLDUtils
import info.weboftrust.ldsignatures.LdProof
import info.weboftrust.ldsignatures.crypto.provider.Ed25519Provider
import info.weboftrust.ldsignatures.crypto.provider.impl.TinkEd25519Provider
import info.weboftrust.ldsignatures.jsonld.LDSecurityContexts
import info.weboftrust.ldsignatures.signer.Ed25519Signature2018LdSigner
import info.weboftrust.ldsignatures.verifier.Ed25519Signature2018LdVerifier
import org.json.JSONObject
import java.net.URI
import java.util.*

object CredentialService {

    val kms = KeyManagementService

    init {
        Ed25519Provider.set(TinkEd25519Provider())
    }

    fun signEd25519Signature2018(issuerDid: String, domain: String, nonce: String?, jsonCred: String): LdProof {

        val jsonLdObject: JsonLDObject = JsonLDObject.fromJson(jsonCred)
        val confLoader = LDSecurityContexts.DOCUMENT_LOADER as ConfigurableDocumentLoader
        confLoader.isEnableHttp = true
        confLoader.isEnableHttps = true
        confLoader.isEnableFile = true
        confLoader.isEnableLocalCache = false
        jsonLdObject.documentLoader = LDSecurityContexts.DOCUMENT_LOADER

        // TODO set current date
        val created = JsonLDUtils.DATE_FORMAT.parse("2017-10-24T05:33:31Z")

        val issuerKeys = kms.loadKeys(kms.getKeyId(issuerDid)!!)

        // following is working in version 0.4
        // var signer = Ed25519Signature2020LdSigner(issuerKeys!!.getPrivateAndPublicKey())
        var signer = Ed25519Signature2018LdSigner(issuerKeys!!.getPrivateAndPublicKey())

        signer.creator = URI.create(issuerDid)
        signer.created = created
        signer.domain = domain
        signer.nonce = nonce
        return signer.sign(jsonLdObject)
    }

    fun addProof(credMap: Map<String, String>, ldProof: LdProof): String {
        val signedCredMap = HashMap<String, Any>(credMap)
        signedCredMap.put("proof", JSONObject(ldProof.toJson()))
        return JSONObject(signedCredMap).toString()
    }

    fun verifyEd25519Signature2018(issuerDid: String, vc: String): Boolean {
        val jsonLdObject = JsonLDObject.fromJson(vc)
        val issuerKeys = kms.loadKeys(kms.getKeyId(issuerDid)!!)
        // following is working in version 0.4
        // val verifier = Ed25519Signature2020LdVerifier(issuerKeys!!.publicKey)
        val verifier = Ed25519Signature2018LdVerifier(issuerKeys!!.publicKey)
        return verifier.verify(jsonLdObject)
    }
}
