package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.model.Did
import id.walt.services.did.DidOptions
import id.walt.services.ecosystems.iota.IotaService

class DidIotaFactory : DidFactory {
    override fun create(key: Key, options: DidOptions?): Did = let {
        if (key.algorithm != KeyAlgorithm.EdDSA_Ed25519) throw IllegalArgumentException("did:iota can not be created with an ${key.algorithm} key.")
        IotaService.createDid(key.keyId.id)
    }
}
