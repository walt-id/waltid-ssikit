package id.walt.services.did.builders

import id.walt.services.did.DidOptions

interface DidBuilder {
    fun create(keyAlias: String? = null, options: DidOptions? = null)
}
