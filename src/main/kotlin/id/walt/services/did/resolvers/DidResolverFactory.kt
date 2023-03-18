package id.walt.services.did.resolvers

import id.walt.model.DidMethod
import id.walt.model.DidUrl
import id.walt.services.WaltIdServices
import id.walt.services.ecosystems.iota.IotaWrapper
import id.walt.services.key.KeyService
import io.ktor.client.*

class DidResolverFactory(
    private val httpNoAuth: HttpClient = WaltIdServices.httpNoAuth,
    private val httpAuth: HttpClient = WaltIdServices.httpWithAuth,
    private val keyService: KeyService,
    private val iotaWrapper: IotaWrapper,
) {

    fun create(didUrl: DidUrl): DidResolver {
        return when (DidMethod.valueOf(didUrl.method)) {
            DidMethod.key -> DidKeyResolver()
            DidMethod.web -> DidWebResolver(httpNoAuth)
            DidMethod.ebsi -> DidEbsiResolver(httpNoAuth, keyService)
            DidMethod.jwk -> DidJwkResolver()
            DidMethod.iota -> DidIotaResolver(iotaWrapper)
            DidMethod.cheqd -> DidCheqdResolver(httpNoAuth)
        }
    }
}
