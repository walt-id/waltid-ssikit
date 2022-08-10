package id.walt.services.iota

import id.walt.crypto.toBase64
import id.walt.model.Did
import id.walt.model.DidIota
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import java.security.interfaces.EdECPrivateKey

object IotaService {
  val iotaWrapper = IotaWrapper.createInstance()

  fun createDid(keyId: String? = null): DidIota {
    val privKeyBytes = keyId?.let { KeyService.getService().load(it, KeyType.PRIVATE).keyPair?.private }?.let {
      (it as EdECPrivateKey).bytes.orElse(null)
    }
    val ptr = iotaWrapper.create_did(privKeyBytes, privKeyBytes?.size?.toLong() ?: 0)
    if(ptr.address() != 0L) {
      val doc = ptr.getString(0)
      iotaWrapper.free_str(ptr)
      return (Did.decode(doc) ?: throw Exception("Error parsing did:iota document")) as DidIota
    } else {
      throw Exception("Error creating did:iota")
    }
  }

  fun resolveDid(did: String): DidIota? {
    val ptr = iotaWrapper.resolve_did(did)
    if(ptr.address() != 0L) {
      val doc = ptr.getString(0)
      iotaWrapper.free_str(ptr)
      return Did.decode(doc)?.let { it as DidIota }
    }
    return null
  }
}
