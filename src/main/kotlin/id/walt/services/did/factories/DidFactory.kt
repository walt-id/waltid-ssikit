package id.walt.services.did.factories

import id.walt.crypto.Key
import id.walt.model.Did
import id.walt.services.did.DidOptions

interface DidFactory {
    fun create(key: Key, options: DidOptions? = null): Did
}
