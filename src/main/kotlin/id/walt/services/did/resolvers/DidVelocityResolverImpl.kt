package id.walt.services.did.resolvers

import id.walt.model.DidVelocity
import id.walt.services.velocitynetwork.VelocityClient

class DidVelocityResolverImpl : DidResolverBase<DidVelocity>() {

    override fun resolve(did: String) = VelocityClient.resolveDid(did)

}