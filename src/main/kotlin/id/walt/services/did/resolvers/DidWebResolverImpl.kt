package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.DidWeb
import id.walt.services.WaltIdServices
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class DidWebResolverImpl : DidResolverBase<DidWeb>() {

    override fun resolve(did: String) = resolveDidWeb(DidUrl.from(did)) as DidWeb

    private fun resolveDidWeb(didUrl: DidUrl): Did = runBlocking {
        log.debug { "Resolving DID $didUrl" }
        val didDocUri = DidWeb.getDidDocUri(didUrl)
        log.debug { "Fetching DID from $didDocUri" }
        val didDoc = WaltIdServices.httpNoAuth.get(didDocUri.toString()).bodyAsText()
        log.debug { didDoc }
        return@runBlocking Did.decode(didDoc)!!
    }

}