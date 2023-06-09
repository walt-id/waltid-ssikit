package id.walt.services.did.factories

import com.nimbusds.jose.jwk.JWK
import id.walt.common.convertToRequiredMembersJsonString
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.encodeMultiBase58Btc
import id.walt.model.DID_CONTEXT_URL
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.VerificationMethod
import id.walt.model.did.DidKey
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidOptions
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import org.erdtman.jcs.JsonCanonicalizer

class DidKeyJwkJcsPubFactory(
    private val keyService: KeyService,
): DidFactoryBase() {
    override fun create(key: Key, options: DidOptions?): Did = let {
        val jwk = keyService.toJwk(key.keyId.id, KeyType.PUBLIC)
        val reqJwkStr = convertToRequiredMembersJsonString(jwk)
        val reqJwk = JWK.parse(reqJwkStr)
        println("jwk-pub-req: ${reqJwk.toJSONString()}")
        println("thumbprint: ${reqJwk.computeThumbprint()}")
        val identifier = JsonCanonicalizer(reqJwkStr).encodedUTF8.encodeMultiBase58Btc()

        val didUrlStr = DidUrl("key", identifier).did
        val kid = didUrlStr + "#" + key.keyId
        val verificationMethods = buildVerificationMethods(key, kid, didUrlStr)
        val keyRef = listOf(VerificationMethod.Reference(kid))
        DidKey(DID_CONTEXT_URL, didUrlStr, verificationMethods, keyRef, keyRef)
    }
}

fun main(){
    ServiceMatrix("service-matrix.properties")
    val key = CryptoService.getService().generateKey(KeyAlgorithm.ECDSA_Secp256r1)
    val jwk = KeyService.getService().toJwk(key.id, KeyType.PUBLIC)
    val reqJwkStr = convertToRequiredMembersJsonString(jwk)
    val identifier = JsonCanonicalizer(reqJwkStr).encodedUTF8.encodeMultiBase58Btc()
    val didUrlStr = "did:key:$identifier"
    println("did: $didUrlStr")
}
