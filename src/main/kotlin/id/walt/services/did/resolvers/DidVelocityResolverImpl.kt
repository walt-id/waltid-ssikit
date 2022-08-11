package id.walt.services.did.resolvers

import com.beust.klaxon.Klaxon
import id.walt.model.Did
import id.walt.model.DidVelocity
import id.walt.services.velocitynetwork.VelocityClient

class DidVelocityResolverImpl : DidResolverBase<DidVelocity>() {

    override fun resolve(did: String) = VelocityClient.resolveDid(did).let {
        Klaxon().parse<Did>(did) ?: error("Unexpected value:\n$did")
    }

}