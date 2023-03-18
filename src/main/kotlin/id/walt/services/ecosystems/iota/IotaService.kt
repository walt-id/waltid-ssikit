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
            throw Exception("did:iota only supports keys of type ${KeyAlgorithm.EdDSA_Ed25519}")
        }

        val privKeyBytes = privKey.keyPair?.private?.let {
            (it as EdECPrivateKey).bytes.orElse(null)
        } ?: throw Exception("Couldn't get private key bytes")
        val ptr = iotaWrapper.create_did(privKeyBytes, privKeyBytes.size.toLong())
        if (ptr.address() != 0L) {
            val doc = ptr.getString(0)
            iotaWrapper.free_str(ptr)
            return (Did.decode(doc) ?: throw Exception("Error parsing did:iota document")) as DidIota
        } else {
            throw Exception("Error creating did:iota")
        }
    }

    fun resolveDid(did: String): Did = iotaWrapper.resolve_did(did).takeIf { it.address() != 0L }?.let {
        val doc = it.getString(0)
        iotaWrapper.free_str(it)
        Did.decode(doc)
    } ?: throw Exception("Could not resolve $did")
}
