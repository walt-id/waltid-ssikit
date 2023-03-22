package id.walt.services.did.resolvers

import id.walt.model.Did
import id.walt.model.DidUrl
import id.walt.model.did.DidWeb
import id.walt.services.did.DidOptions
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking

class DidWebResolver(private val httpClient: HttpClient) : DidResolverBase<DidWeb>() {

    override fun resolve(didUrl: DidUrl, options: DidOptions?) = resolveDidWeb(DidUrl.from(didUrl.did)) as DidWeb

    private fun resolveDidWeb(didUrl: DidUrl): Did = runBlocking {
        log.debug { "Resolving DID $didUrl" }
        val didDocUri = DidWeb.getDidDocUri(didUrl)
        log.debug { "Fetching DID from $didDocUri" }
        val didDoc = httpClient.get(didDocUri.toString()).bodyAsText()
        log.debug { didDoc }
        return@runBlocking Did.decode(didDoc)!!
    }

}
