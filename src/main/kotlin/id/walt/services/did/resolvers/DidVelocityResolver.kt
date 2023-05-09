package id.walt.services.did.resolvers


import com.beust.klaxon.Klaxon
import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidVelocity
import id.walt.services.did.DidOptions
import id.walt.services.ecosystems.velocity.VelocityClient

class DidVelocityResolver : DidResolverBase<DidVelocity>() {

    override fun resolve(didUrl: DidUrl, options: DidOptions?): Did = VelocityClient.resolveDid(didUrl.did).let {
        Klaxon().parse<Did>(didUrl.did) ?: error("Unexpected value:\n${didUrl.did}")
    }

}
