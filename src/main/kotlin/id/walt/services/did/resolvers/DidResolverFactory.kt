package id.walt.services.did.resolvers

import id.walt.model.DidMethod
import id.walt.model.DidUrl

object DidResolverFactory {
    fun create(didUrl: DidUrl): DidResolver {
        return when (didUrl.method) {
            DidMethod.key.name -> DidKeyResolverImpl()
            DidMethod.web.name -> DidWebResolverImpl()
            DidMethod.ebsi.name -> DidEbsiResolverImpl()
            DidMethod.velocity.name -> DidVelocityResolverImpl()
            else -> TODO("did:${didUrl.method} not implemented yet")
        }
    }
}