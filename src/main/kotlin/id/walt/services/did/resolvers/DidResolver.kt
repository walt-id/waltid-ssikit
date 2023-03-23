package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.services.did.DidOptions
import mu.KotlinLogging.logger

interface DidResolver {
    fun resolve(didUrl: DidUrl, options: DidOptions? = null): Did
}

abstract class DidResolverBase<T : Did> : DidResolver {
    protected val log = logger {}
}
