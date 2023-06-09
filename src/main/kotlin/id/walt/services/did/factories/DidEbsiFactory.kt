package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.crypto.KeyId
import id.walt.model.DID_CONTEXT_URL
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.VerificationMethod
import id.walt.model.did.DidEbsi
import id.walt.services.did.DidEbsiCreateOptions
import id.walt.services.did.DidOptions
import id.walt.services.did.DidService.resolve
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType

class DidEbsiFactory(
    private val keyService: KeyService,
) : DidFactoryBase() {
    override fun create(key: Key, options: DidOptions?): Did {
        return when ((options as? DidEbsiCreateOptions)?.version ?: 1) {
            1 -> createDidEbsiV1(key)
            2 -> createDidEbsiV2(key.keyId)
            else -> throw Exception("Did ebsi version must be 1 or 2")
        }
    }

    private fun createDidEbsiV1(key: Key) = let {
        val didUrl = DidUrl.generateDidEbsiV1DidUrl()
        val kid = didUrl.did + "#" + key.keyId
        val verificationMethods = buildVerificationMethods(key, kid, didUrl.did)
        val keyRef = listOf(VerificationMethod.Reference(kid))
        DidEbsi(
            listOf(DID_CONTEXT_URL), // TODO Context not working "https://ebsi.org/ns/did/v1"
            didUrl.did, verificationMethods, keyRef, keyRef
        )
    }

    private fun createDidEbsiV2(keyId: KeyId) = let {
        val publicKeyJwk = keyService.toJwk(keyId.id, KeyType.PUBLIC)
        val publicKeyThumbprint = publicKeyJwk.computeThumbprint()
        resolve(DidUrl.generateDidEbsiV2DidUrl(publicKeyThumbprint.decode()))
    }
}
