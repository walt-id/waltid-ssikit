package id.walt.services.did.factories

import com.nimbusds.jose.util.Base64URL
import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.services.did.DidOptions
import id.walt.services.did.DidService.resolve
import id.walt.services.key.KeyService

class DidJwkFactory(
    private val keyService: KeyService,
) : DidFactoryBase() {
    override fun create(key: Key, options: DidOptions?): Did =
        resolve("did:jwk:${Base64URL.encode(keyService.toJwk(key.keyId.id).toString())}")
}
