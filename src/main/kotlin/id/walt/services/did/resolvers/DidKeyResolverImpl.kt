package id.walt.services.did.resolvers

import id.walt.crypto.convertMultiBase58BtcToRawKey
import id.walt.crypto.getKeyAlgorithmFromMultibase
import id.walt.model.Did
import id.walt.model.DidKey
import id.walt.model.DidUrl
import id.walt.services.did.DidService

class DidKeyResolverImpl : DidResolverBase<DidKey>() {

    override fun resolve(did: String) = resolveDidKey(DidUrl.from(did))

    private fun resolveDidKey(didUrl: DidUrl): Did {
        val keyAlgorithm = getKeyAlgorithmFromMultibase(didUrl.identifier)
        val pubKey = convertMultiBase58BtcToRawKey(didUrl.identifier)
        return DidService.constructDidKey(didUrl, pubKey, keyAlgorithm)
    }

}