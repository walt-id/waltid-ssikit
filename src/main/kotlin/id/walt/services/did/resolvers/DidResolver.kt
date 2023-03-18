package id.walt.services.did.resolvers

import id.walt.model.Did
import mu.KotlinLogging.logger

interface DidResolver {
    fun resolve(did: String): Did
}

abstract class DidResolverBase<T : Did> : DidResolver {
    protected val log = logger {}
}
