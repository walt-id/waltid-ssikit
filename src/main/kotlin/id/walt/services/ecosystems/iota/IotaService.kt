package id.walt.services.ecosystems.iota

import id.walt.crypto.KeyAlgorithm
import id.walt.model.Did
import id.walt.model.did.DidIota
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import java.security.interfaces.EdECPrivateKey

object IotaService {
    val iotaWrapper = IotaWrapper.createInstance()

    fun createDid(keyId: String): DidIota {
        // TODO: implement iota key store interface, to avoid exposing private key!
        val privKey = KeyService.getService().load(keyId, KeyType.PRIVATE)
        if (privKey.algorithm != KeyAlgorithm.EdDSA_Ed25519) {
            throw UnsupportedOperationException("did:iota only supports keys of type ${KeyAlgorithm.EdDSA_Ed25519}")
        }

        val privKeyBytes = privKey.keyPair?.private?.let {
            (it as EdECPrivateKey).bytes.orElse(null)
        } ?: throw IllegalArgumentException("Couldn't get private key bytes")
        val ptr = iotaWrapper.create_did(privKeyBytes, privKeyBytes.size.toLong())
        if (ptr.address() != 0L) {
            val doc = ptr.getString(0)
            iotaWrapper.free_str(ptr)
            return (Did.decode(doc) ?: throw IllegalArgumentException("Error parsing did:iota document")) as DidIota
        } else {
            throw IllegalStateException("Error creating did:iota")
        }
    }
}
