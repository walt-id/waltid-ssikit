package id.walt.services.did.factories

import com.beust.klaxon.Klaxon
import id.walt.common.convertToRequiredMembersJsonString
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.convertRawKeyToMultiBase58Btc
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
        val reqJwk = convertToRequiredMembersJsonString(jwk)
        val identifier = convertRawKeyToMultiBase58Btc(JsonCanonicalizer(Klaxon().toJsonString(reqJwk)).encodedUTF8, 0xeb51u)
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
    val reqJwk = convertToRequiredMembersJsonString(jwk)
    val identifier = convertRawKeyToMultiBase58Btc(JsonCanonicalizer(Klaxon().toJsonString(reqJwk)).encodedUTF8, 0xeb51u)
    val didUrlStr = "did:key:$identifier"
    println("did: $didUrlStr")
}
