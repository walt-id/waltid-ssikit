package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.services.did.DidCheqdCreateOptions
import id.walt.services.did.DidOptions
import id.walt.services.ecosystems.cheqd.CheqdService

class DidCheqdFactory : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did =
        CheqdService.createDid(key.keyId.id, (options as? DidCheqdCreateOptions)?.network ?: "testnet")
}
